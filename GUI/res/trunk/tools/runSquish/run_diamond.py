import os
import re
import sys
import time
import getpass
import optparse

from xlib import xTools

__author__ = 'syan'

DIAMOND_EXTERNAL_ENV_KEY = "EXTERNAL_DIAMOND_PATH"
SQUISH_EXTERNAL_ENV_KEY = "EXTERNAL_SQUISH_PATH"

# RUN_SQUISH = '''#!/bin/sh
# %(set_path)s
# %(run_server)s
# %(sleep)s
# %(squishserver)s --config addAUT %(name_of_aut)s "%(path_to_aut)s"
# %(squishrunner)s --testcase %(testcase)s --lang %(language)s --wrapper %(wrapper)s --objectmap %(objectmap)s --reportgen xml2.1,"%(reportgen)s" --aut %(name_of_aut)s
# %(squishserver)s --stop
# '''


RUN_SQUISH = '''#!/bin/sh
%(set_path)s
%(run_server)s
%(sleep)s
%(squishserver)s --config addAUT %(name_of_aut)s "%(path_to_aut)s"
%(squishrunner)s --config setAUTTimeout 60
%(squishrunner)s --config setResponseTimeout 60
%(squishrunner)s --testcase %(testcase)s --lang %(language)s --wrapper %(wrapper)s --objectmap %(objectmap)s --aut %(name_of_aut)s
%(squishserver)s --stop
'''



def get_real_value(my_string, env_key):
    if my_string:
        return my_string
    return os.getenv(env_key)

def get_focus(a_file, pattern):
    foo = xTools.simple_parser(a_file, [pattern])
    if not foo:
        return ""
    else: # foo = line, m
        return foo[1].group(1)

def get_squish_file(testdata_path):
    map_files, conf_files = list(), list()
    for foo in os.listdir(testdata_path):
        fext = xTools.get_fext_lower(foo)
        abs_foo = os.path.join(testdata_path, foo)
        if fext == ".map":
            map_files.append(abs_foo)
        elif fext == ".conf":
            conf_files.append(abs_foo)
    return map_files, conf_files

class RunSquishCase:
    def __init__(self):
        _xlib = os.path.join(xTools.get_file_dir(sys.argv[0]), "xlib")
        self.xlib = os.path.abspath(_xlib)

    def process(self):
        xTools.say_it("---- Start running Squish test case ...")
        sts = self._process()
        xTools.say_it("---- End of running Squish test case ...")
        return sts

    def _process(self):
        if self.run_option_parser():
            return 1
        if self.copy_layout_file():
            return 1
        if self.run_batch_file():
            return 1

    def run_option_parser(self):
        parser = optparse.OptionParser()
        parser.add_option("--top-dir", help="specify top source path")
        parser.add_option("--design", help="specify design name")
        parser.add_option("--diamond", help="specify Diamond path")
        parser.add_option("--squish", help="specify Squish path")
        parser.add_option("--x86", action="store_true", help="run with x86 build")
        opts, args = parser.parse_args()
        self.top_dir = opts.top_dir
        self.design = opts.design
        self.diamond = opts.diamond
        self.squish = opts.squish
        self.x86 = opts.x86
        self.on_win, self.nt_lin = xTools.get_os_name(self.x86)
        return self._check_options()

    def _check_options(self):
        if not self.top_dir:
            self.top_dir = os.getcwd()
        else:
            if xTools.not_exists(self.top_dir, "Top Source Path"):
                return 1
        self.design = xTools.get_abs_path(self.design, self.top_dir)
        if xTools.not_exists(self.design, "Design absolute path"):
            return 1

        self.diamond = get_real_value(self.diamond, DIAMOND_EXTERNAL_ENV_KEY)
        self.squish = get_real_value(self.squish, SQUISH_EXTERNAL_ENV_KEY)

        if xTools.not_exists(self.diamond, "Diamond Path"):
            return 1
        if xTools.not_exists(self.squish, "Squish Path"):
            return 1
        if self.on_win:
            self.squish_server = os.path.join(self.squish, "bin", "squishserver.exe")
            self.squish_runner = os.path.join(self.squish, "bin", "squishrunner.exe")
        else:
            self.squish_server = os.path.join(self.squish, "bin", "squishserver")
            self.squish_runner = os.path.join(self.squish, "bin", "squishrunner")

    def copy_layout_file(self):
        user = getpass.getuser()
        testdata_path = os.path.join(self.design, "testdata")
        if xTools.not_exists(testdata_path, "testdata path"):
            return 1
        if self.on_win:
            layout_path = os.path.join(r"C:\Users\%s\AppData\Roaming\LatticeSemi\pnlayout" % user)
        else:
            layout_path = "/users/%s/.config/LatticeSemi/pnlayout" % user
        for foo in os.listdir(testdata_path):
            if xTools.get_fext_lower(foo) == ".ini": # found layout file
                src_foo = os.path.join(testdata_path, foo)
                dst_foo = os.path.join(layout_path, foo)
                if xTools.wrap_cp_file(src_foo, dst_foo):
                    return 1

    def run_batch_file(self):
        testrun_path = os.path.join(self.design, "_scratch_cmd")
        if xTools.wrap_md(testrun_path, "TestRun Path"):
            return 1
        _recov = xTools.ChangeDir(testrun_path)
        squish_map_files, squish_conf_files = get_squish_file(os.path.join(self.design, "testdata"))
        if len(squish_map_files) != 1:
            xTools.say_it("Error. Found map files: %s" % squish_map_files)
            return 1
        if len(squish_conf_files) != 1:
            xTools.say_it("Error. Found conf files: %s" % squish_conf_files)
            return 1

        batch_kwargs = {"squishserver" : self.squish_server, "squishrunner" : self.squish_runner,
                        "path_to_aut" : os.path.join(self.diamond, "bin", self.nt_lin),
                        "testcase" : self.design,
                        "reportgen" : os.path.abspath("report_%s.xml" % time.strftime("%m%d%H%M")),
                        "objectmap" : squish_map_files[0],
                        "language" : get_focus(squish_conf_files[0], re.compile("LANGUAGE=(\w+)", re.I)),
                        "wrapper" : get_focus(squish_conf_files[0], re.compile("WRAPPERS=(\w+)", re.I)),
                        # ------------
                        }

        if self.on_win:
            batch_kwargs["set_path"] = r'set PATH=%%PATH%%;%s;%s\bin' % (os.path.join(self.diamond, "bin", self.nt_lin), self.squish)
            batch_kwargs["run_server"] = r'start "Squishserver Window" /B "%s" --verbose' % self.squish_server
            batch_kwargs["sleep"] = "%s 5" % os.path.join(self.xlib, "sleep.exe")
            batch_kwargs["name_of_aut"] = "pnmain.exe"
        else:
            batch_kwargs["set_path"] = r'set PATH=$PATH:%s:%s/bin' % (os.path.join(self.diamond, "bin", self.nt_lin), self.squish)
            batch_kwargs["run_server"] = "%s &" % self.squish_server
            batch_kwargs["sleep"] = "sleep 5"
            batch_kwargs["name_of_aut"] = "diamond"

        batch_file = "run_squish.bat"
        xTools.write_file("run_squish.bat", RUN_SQUISH % batch_kwargs)
        if self.on_win:
            sts, txt = xTools.get_status_output(batch_file)
        else:
            sts, txt = xTools.get_status_output("sh %s" % batch_file)
        xTools.say_it("Status: %s" % sts)
        xTools.say_it(txt, "Detail:")
        xTools.write_file("runtime_console.log", txt)
        check_sts = get_final_status(txt)
        _recov.comeback()
        return check_sts

def get_final_status(a_list):
    p = re.compile("^Final check status:(.+)")
    for line in a_list:
        line = line.strip()
        m = p.search(line)
        if m:
            try:
                return int(m.group(1))
            except ValueError:
                return 0
    return 0

if __name__ == "__main__":
    my_tst = RunSquishCase()
    sts = my_tst.process()
    sys.exit(sts)



