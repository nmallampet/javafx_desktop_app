import os
import re
import sys
import shlex
import optparse
import win32api

from xlib.xOS import wrap_md, not_exists, wrap_copy_dir, wrap_copy_file
from xlib.xOS import  get_hot_files, get_unique_file, get_unix_path, RecoverPath
from xlib.xFamily import get_std_family_vendor, get_family_by_vendor
from xlib.xLog import set_logging_level, print_error, print_always, wrap_debug
from xlib.xDevkit import ParseDevkit
from xlib.xTools import parse_file, append_file

__author__ = 'syan'

class Options:
    def parse_options(self):
        _group = ("ecp2_60", "large_25", "sc_24", "xo_103")
        _family =  get_family_by_vendor(vendor="") # all vendors
        parser = optparse.OptionParser()
        parser.add_option("-D", "--debug", action="store_true", help="print debug message")
        parser.add_option("-s", "--from-dir", help="specify source path name")
        parser.add_option("-d", "--to-dir", help="specify destination path name")
        parser.add_option("-f", "--family", type="choice", choices=_family, help="specify family name")
        parser.add_option("-n", "--design", help="specify design name")
        parser.add_option("-g", "--group", type="choice", choices=_group, help="specify group name")

        opts, args = parser.parse_args()

        self.debug = opts.debug
        self.from_dir = opts.from_dir
        self.to_dir = opts.to_dir
        self.family = opts.family
        self.design = opts.design
        self.group = opts.group

        # sanity check
        return self.check_options()

    def check_options(self):
        # set logging level
        set_logging_level(self.debug)

        # check arguments
        if not_exists(self.from_dir, "source path"):
            return 1
        self.from_dir = os.path.abspath(self.from_dir)

        if wrap_md(self.to_dir, "destination path"):
            return 1
        self.to_dir = os.path.abspath(self.to_dir)

        if not self.group:
            print_error("No group name specified")
            return 1

        if not self.family:
            print_error("No family name specified")
            return 1

        # standardize family name and vendor name
        self.family, self.vendor = get_std_family_vendor(self.family)

class GetFmaxSweepingRange:
    def __init__(self):
        self.group = ""

    def create_range_dict(self):
        if self.group == "ecp2_60":
            self.range_dict = dict(default="50,300,10", ADPCMtest="10,50,5", adpcm_codec_64ch="10,50,5")
        elif self.group == "sc_24":
            self.range_dict = dict(default="100,400,10")
        elif self.group == "large_25":
            self.range_dict = dict(default="50,300,10",
                g64                   = "10,100,5",
                MARS128_iter          = "10,100,5",
                MARS3in1_iter_42k     = "10,100,5",
                RC6128_iter           = "10,100,5",
                RC63in1_iter          = "10,100,5",
                RIJNDAEL128_pipe_65k  = "10,100,5",
                SERPENT_pipe_68k      = "10,100,5",
                TWOFISH128_pipe       = "10,100,5",
                TWOFISH3in1_pipe_72k  = "10,100,5",
                sp7                   = "10,100,5",
                typhoon               = "10,100,5",
                hdlc                  = "10,100,5",
                nozawa_top            = "10,100,5")
        elif self.group == "xo_103":
            self.range_dict = dict(default="50,400,10")
        else:  # never got here
            print_error("Unknown group name: %s" % self.group)
            return 1

    def get_design_range(self, design):
        default_range=self.range_dict.get("default")
        return self.range_dict.get(design, default_range)

class GetDesignDict:
    def __init__(self, top_dir, design, family):
        self.top_dir = top_dir
        self.design = design
        self.family = family
        self.mps_list = ("models", "par", "synthesis")

    def _is_design(self, a_design, test_name="par"):
        return os.path.isdir(os.path.join(self.top_dir, a_design, test_name))

    def get_design_list(self):
        self.design_list = list()
        if self.design:
            if self._is_design(self.design):
                self.design_list.append(self.design)
            else:
                print_error("Check design %s" % self.design)
        else:
            for foo in os.listdir(self.top_dir):
                if self._is_design(foo):
                    self.design_list.append(foo)
        if not self.design_list:
            print_error("Not found any design under %s" % self.top_dir)
            return 1
        wrap_debug(self.design_list, "Design List:")

    def get_design_dict(self):
        if self.get_design_list():
            return

        design_dict = dict()
        for design in self.design_list:
            design_dir = os.path.join(self.top_dir, design)
            design_dir = win32api.GetLongPathName(win32api.GetShortPathName(design_dir))
            design = os.path.basename(design_dir)  # use the real letter for design name
            for i, mps in enumerate(self.mps_list):
                mps_dir = os.path.join(design_dir, mps)
                if not_exists(mps_dir):
                    if not i:  # models
                        continue
                    else:
                        print_error("Not found %s" % mps_dir)
                        break
                mps_fam_dir = self.get_fam_dir(mps_dir)
                if mps_fam_dir:
                    if design_dict.has_key(design):
                        design_dict[design][mps] = mps_fam_dir
                    else:
                        design_dict[design] = {mps : mps_fam_dir}
        if not design_dict:
            print_error("Not found cases for family %s" % self.family)
            return
        wrap_debug(design_dict, "Design Dictionary:")
        return design_dict

    def get_fam_dir(self, mps_dir):
        for foo in os.listdir(mps_dir):
            cur_fam = get_std_family_vendor(foo)[0]
            if not cur_fam:
                continue
            if cur_fam == self.family:
                return os.path.join(mps_dir, foo)

class ParseRunOptionsFile:
    def __init__(self, src_path, dst_path, design, family, run_options_file):
        self.src_path = src_path
        self.dst_path = dst_path
        self.design = design
        self.family = family
        self.run_options_file = run_options_file

        self.p_models_family = re.compile(".+/%s/models/[^/]+" % design, re.I)
        self.p_source = re.compile(".+/%s/source" % design, re.I)

        self.hdl_info = list()
        self.inc_path = set()

    def get_hdl_info(self):
        return self.hdl_info

    def get_inc_path(self):
        return self.inc_path

    def process(self):
        recover = RecoverPath(self.dst_path)
        self.get_raw_info()
        status = 0
        if self.create_hdl_info():
            status = 1
        if not status:
            status = self.create_inc_path()
        recover.run()
        return status

    def get_raw_info(self):
        self.raw_hdl, self.raw_inc = list(), set()
        for line in open(self.run_options_file):
            line = line.strip()
            if not line: continue
            line_list = shlex.split(line)
            if line_list[0] == "add_file":
                self.raw_hdl.append(line_list)
            elif line_list[0] == "set_option":
                if line_list[1] == "-include_path":
                    inc_path = re.sub("{|}", "", line_list[-1])
                    inc_path = re.split(";", inc_path)
                    for item in inc_path:
                        self.raw_inc.add(item)
        wrap_debug(self.raw_hdl, "Raw HDL files:")
        if self.raw_inc:
            wrap_debug(self.raw_inc, "Raw Include paths:")

    def get_real_path(self, a_path):
        a_path = os.path.abspath(a_path)
        a_path = get_unix_path(a_path)
        a_path = self.p_models_family.sub("./models/%s" % self.family, a_path)
        a_path = self.p_source.sub("./source", a_path)
        if not_exists(a_path): # sdc file maybe
            a_path = "./others/%s/%s" % (self.family, os.path.basename(a_path))
        if not_exists(a_path, "source path/file"):
            return
        a_path = win32api.GetLongPathName(win32api.GetShortPathName(a_path))
        return a_path

    def create_hdl_info(self):
        if not self.raw_hdl:
            print_error("Not found hdl source file list")
            return 1
        for hdl_list in self.raw_hdl:
            try:
                lib_idx = hdl_list.index("-lib")
                lib_name = hdl_list[lib_idx+1]
                if lib_name == "work":
                    lib_name = ""
            except ValueError:
                lib_name = ""
            real_file = self.get_real_path(hdl_list[-1])
            if not real_file:
                return 1
            if lib_name:
                real_file = "-lib %s %s" % (lib_name, real_file)
            self.hdl_info.append(real_file)
        wrap_debug(self.hdl_info, "HDL information:")

    def create_inc_path(self):
        if not self.raw_inc:
            return
        for inc in self.raw_inc:
            real_path = self.get_real_path(inc)
            self.inc_path.add(real_path)
        wrap_debug(self.inc_path, "Include Path:")


class ConvertDesigns(Options, GetFmaxSweepingRange):
    def __init__(self):
        GetFmaxSweepingRange.__init__(self)

    def process(self):
        if self.parse_options():
            return 1
        #
        if self.create_range_dict():
            return 1

        self.create_patterns()

        if self.create_dev_parser():
            return 1

        if self.create_design_dict():
            return 1

        if self.convert_designs():
            return 1

    def create_patterns(self):
        """
        Create patterns for parsing device and top_module from the project file and srr file
        """
        # <BaliProject version="1.3" title="ELSA_48E_synp_ecp4" device="LFE4-190E-9FLG1152C"
        ldf_pattern = dict(devkit = re.compile('BaliProject.+\s+device="([^"]+)"', re.I))
        # DEVKIT LFE3-150EA-8FN1156CES
        syn_pattern = dict(devkit = re.compile('^DEVKIT\s+(.+)', re.I))

        # DEVFAM   VIRTEX4
        # DEVICE   xc4vlx80
        # DEVPKG   ff1148
        # DEVSPEED   -12
        cfg_pattern = dict(devfam = re.compile("DEVFAM\s+(.+)", re.I),
            device = re.compile("DEVICE\s+(.+)", re.I),
            devpkg = re.compile("DEVPKG\s+(.+)", re.I),
            devspeed = re.compile("DEVSPEED\s+(.+)", re.I))

        # set_global_assignment -name FAMILY "Arria II GX"
        # set_global_assignment -name DEVICE EP2AGX95EF35I3
        qsf_pattern = dict(family = re.compile('set_global_assignment\s+-name\s+FAMILY\s+(.+)', re.I),
            device = re.compile('set_global_assignment\s+-name\s+DEVICE\s+(.+)', re.I))

        patterns = {"lattice" : [(".syn", syn_pattern), (".ldf", ldf_pattern)],
                    "xilinx"  : [(".cfg", cfg_pattern)],
                    "altera"  : [(".qsf", qsf_pattern)]}
        self.patterns = patterns.get(self.vendor)

        self.srr_pattern = {"top_module":  [re.compile("Top\s+view:\s+(\S+)", re.I)]}
        # the REAL TOP MODULE name should be found by "Top view"
        #                                    re.compile("Top\s+entity\s+is\s+set\s+to\s+(\S+)\.", re.I),
        #                                    re.compile("Selecting\s+top\s+level\s+module\s+(\S+)", re.I)]}

    def create_dev_parser(self):
        if self.vendor != "lattice":
            return

        xml_file = os.path.join(sys.argv[0], "..", "..", "conf", "DiamondDevFile.xml")
        self.dev_parser = ParseDevkit(xml_file)
        return self.dev_parser.initialize()

    def create_design_dict(self):
        seeker = GetDesignDict(self.from_dir, self.design, self.family)
        self.design_dict = seeker.get_design_dict()
        if not self.design_dict:
            return 1

    def convert_designs(self):
        design_keys = self.design_dict.keys()
        design_keys.sort(key=str.lower)
        len_keys = len(design_keys)
        for i, design in enumerate(design_keys):
            print_always("Converting (%3d/%-3d): %s" % (i+1, len_keys, design))
            self.cur_design = design
            self.src_mps = self.design_dict.get(design)
            if self.convert_it():
                print_error("Failed to converting %s(%s)" % (self.cur_design, self.family))
            else:
                print_always("convert %s successfully" % self.cur_design)
            print_always("")

    def convert_it(self):
        self.src = os.path.join(self.from_dir, self.cur_design)
        self.dst = os.path.join(self.to_dir, self.cur_design)
        self.src_models = self.src_mps.get("models")
        self.src_par = os.path.join(self.src_mps.get("par"), "synp")
        self.src_synthesis = os.path.join(self.src_mps.get("synthesis"), "synp")
        
        if not_exists(self.src_par, "source par path"):
            return 1
        if not_exists(self.src_synthesis, "source synthesis path"):
            return 1

        if self.src_models:
            self.dst_models = os.path.join(self.dst, "models", self.family)
        else:
            self.dst_models = ""
        self.dst_others = os.path.join(self.dst, "others", self.family)

        if wrap_md(self.dst, "New destination design path"):
            return 1

        if self.copy_readme_file():
            return 1

        if self.copy_source_folder():
            return 1

        if self.copy_models_folder():
            return 1

        if self.copy_others_file():
            return 1

        if self.write_info_file():
            return 1

    def copy_readme_file(self):
        src_readme = os.path.join(self.src, "readme.txt")
        dst_readme = os.path.join(self.dst, "readme.txt")
        if os.path.isfile(src_readme):
            return wrap_copy_file(src_readme, dst_readme)

    def copy_source_folder(self):
        src_source = os.path.join(self.src, "source")
        dst_source = os.path.join(self.dst, "source")
        return wrap_copy_dir(src_source, dst_source)

    def copy_models_folder(self):
        if self.src_models:
            return wrap_copy_dir(self.src_models, self.dst_models)

    def copy_others_file(self):
        self.has_others = 0
        par_hot_files = get_hot_files(self.src_par, [".ngc", ".hex", ".mif"])
        synthesis_hot_files = get_hot_files(self.src_synthesis, [".ldc", ".sdc"])
        hot_files = par_hot_files + synthesis_hot_files
        if not hot_files:
            return
        if wrap_md(self.dst_others, "Destination others path"):
            return 1
        self.has_others = 1
        for hf in hot_files:
            dst_hf = os.path.join(self.dst_others, os.path.basename(hf))
            if wrap_copy_file(hf, dst_hf):
                return 1

    def write_info_file(self):
        if self.get_device_info():
            return 1

        if self.get_top_module():
            return 1

        if self.get_prj_settings():
            return 1

        self.write_it()

    def get_device_info(self):
        for (fext, p) in self.patterns:
            prj_file = get_unique_file([self.src_par, fext])
            if prj_file:
                break
        else:
            print_error("Not found project file in %s" % self.src_par)
            return 1

        self.device_info = parse_file(prj_file, p)

        devkit = self.device_info.get("devkit")
        if devkit:  # lattice device
            devkit_detail = self.dev_parser.get_std_devkit(devkit)
            if not devkit_detail:
                return 1
            self.device_info["devkit"] = devkit_detail.get("name")

    def get_top_module(self):
        srr_file = get_unique_file([os.path.join(self.src_synthesis, "rev_1"), ".srr"])
        if not srr_file:
            return 1
        top_module = parse_file(srr_file, self.srr_pattern, at_once=True)
        if not top_module:
            print_error("Not found top module name in %s" % srr_file)
            return 1
        self.top_module = top_module.get("top_module")

    def get_prj_settings(self):
        run_options_file = os.path.join(self.src_synthesis, "rev_1", "run_options.txt")
        if not_exists(run_options_file, "Synplify run_options file"):
            return 1
        prj_parser = ParseRunOptionsFile(self.src_synthesis, self.dst, self.cur_design, self.family, run_options_file)
        if prj_parser.process():
            return 1

        self.hdl_info = prj_parser.get_hdl_info()
        self.inc_path = prj_parser.get_inc_path()

    def write_it(self):
        info_file = os.path.join(self.dst, "_qa_%s.info" % self.family)
        info_lines = ["[qa]"]
        for key, value in self.device_info.items():
            info_lines.append("%s = %s" % (key, value))
        info_lines.append("top_module = %s" % self.top_module)
        if self.has_others:
            info_lines.append("others_path = ./others/%s" % self.family)
        info_lines.append("fmax_range = %s" % self.get_design_range(self.cur_design))
        info_lines.append("")

        if self.inc_path:
            info_lines.append("inc_path =")
            for item in self.inc_path:
                info_lines.append("    <%s>" % item)
        info_lines.append("src_files =")
        for item in self.hdl_info:
            info_lines.append("    <%s>" % item)

        append_file(info_file, info_lines, append=False)





if __name__ == "__main__":
    test_converter = ConvertDesigns()
    test_converter.process()
