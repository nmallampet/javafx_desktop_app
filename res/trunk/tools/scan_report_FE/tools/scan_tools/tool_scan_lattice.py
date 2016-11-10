import os
import re
import sys
xlib = os.path.join(os.path.dirname(__file__),'..','..','bin','xlib')
xlib = os.path.abspath(xlib)
if xlib in sys.path:
    pass
else:
    sys.path.append(xlib)
from xOS import not_exists, wrap_copy_file, get_fname_ext
from xLog import print_error
from tool_scan_report import ScanBasic
from xTools import time2secs, get_file_line_count
import traceback

def get_clocks_from_mrp_file(mrp_file):
    if not_exists(mrp_file, "Lattice Mrp file"):
        return
    p_clock_number = re.compile("^Number\s+of\s+clocks:\s+(\d+)")
    p_clock = re.compile("^Net\s+([^:]+):")
    p_stop = re.compile("Number of Clock Enables")
    clock_number = 0
    clocks = list()
    for line in open(mrp_file):
        line = line.strip()
        if not clock_number:
            mcn = p_clock_number.search(line)
            if mcn:
                clock_number = int(mcn.group(1))
            continue
        if p_stop.search(line):
            break
        mc = p_clock.search(line)
        if mc:
            clocks.append(mc.group(1))
    if len(clocks) != clock_number:
        print_error("Not found %d clocks in %s" % (clock_number, mrp_file))
        return
    return clocks

p_frequency = re.compile('^FREQUENCY\s+')
def update_clocks(prf_file, clocks, fixed_number):
    if not clocks:
        print_error("Not found any clocks name")
        return 1
    prf_file_bak = prf_file + ".a"
    if not os.path.isfile(prf_file_bak):
        wrap_copy_file(prf_file, prf_file_bak)
    prf_lines = open(prf_file_bak)
    prf_ob = open(prf_file, "w")
    for line in prf_lines:
        line = line.strip()
        if p_frequency.search(line):
            continue
        print >> prf_ob, line
    for clk in clocks:
        print >> prf_ob, 'FREQUENCY NET "%s" %s MHz ;' % (clk, fixed_number)
    prf_ob.close()

p_mhz = re.compile("\S+\s+MHz", re.I)
def update_frequency(prf_file, fixed_number):
    if not_exists(prf_file, "elder prf file"):
        return 1
    prf_file_bak = prf_file + ".b"
    wrap_copy_file(prf_file, prf_file_bak, force=True)
    prf_lines = open(prf_file_bak)
    prf_ob = open(prf_file, "w")
    for line in prf_lines:
        line = line.strip()
        if p_frequency.search(line):
            line = p_mhz.sub("%s MHz" % fixed_number, line)
        print >> prf_ob, line

    prf_ob.close()


class ScanLatticeTwr:
    def __init__(self, pap):
        self.pap = pap  # Performance Achievement Percentage
       
        self.title = [r"Seed/Target","PAP", "PAPFactor", "targetFmax", "fmax", "clkName", "logic", "route", "level", "twrLabel","score_setup","score_hold"]
        self.create_patterns()
        self.all_raw_data = list()
        #Score: 1467 (setup), 0 (hold)
        self.score = re.compile('Score: (\d+) \(setup\), (\d+) \(hold\)')
        self.score_data_setup = 'NA'
        self.score_data_hold = 'NA'
    def create_patterns(self):
        self.p_fre_clk = re.compile('^Preference: FREQUENCY[^"]+"([^"]+)"')
        # Preference: FREQUENCY NET "ck19fb_out_o" 280.000000 MHz ;
        self.p_lrl = re.compile("""\s+\(
                                (?P<logic>[\d\.]+%)\s+logic,\s+
                                (?P<route>[\d\.]+%)\s+route
                                \),\s+
                                (?P<level>\d+)\s+logic\s+level
                                """, re.VERBOSE)
        # Delay:               0.298ns  (81.2% logic, 18.8% route), 2 logic levels.
        if self.pap:
            self.create_pap_patterns()
        else:
            self.create_fmax_patterns()

    def create_pap_patterns(self):
        self.rpt_start = re.compile("Dump PAP Details", re.I)
        # PAP=   48.92% | 280.00 MHz | 137.01 MHz |   1.00 | FREQUENCY NET "spi4_clk_int" 280.000000 MHz ;
        self.rpt_pattern = re.compile('''PAP=\s+
                                      ([\d\.]+%)\s+            # PAP
                                      ([\d\.]+)\s+MHz\s+       # Constraint
                                      ([\d\.]+)\s+MHz\s+       # Actual
                                      ([\d\.]+)\s+             # Factor
                                      FREQUENCY[^"]+
                                      "([^"]+)"                # Clock name
                                      ''', re.VERBOSE | re.I)
        # can prcess the line as:
        # PAP=  131.14% |  80.00 MHz | 104.91 MHz |   1.00 | FREQUENCY 80.000000 MHz ;
        self.rpt_pattern = re.compile('''PAP=\s+
                                      ([\d\.]+%)\s+            # PAP
                                      ([\d\.]+)\s+MHz\s+       # Constraint
                                      ([\d\.]+)\s+MHz\s+       # Actual
                                      ([\d\.]+)\s+             # Factor
                                      FREQUENCY
                                      (.*?)\d+\.                 # Clock name
                                      ''', re.VERBOSE | re.I)
        self.raw_title = ["PAP", "targetFmax", "fmax", "PAPFactor", "clkName"]
        self.rpt_stop = re.compile("Overall Performance")

    def create_fmax_patterns(self):
        self.rpt_start = re.compile("^Report\s+Summary")
        # FREQUENCY NET "fir_clk_c" 300.000000    |             |             |
        # MHz ;                                   |  300.000 MHz|  443.656 MHz|   2
        self.rpt_pattern = re.compile('''FREQUENCY[^"]+
                                      "([^"]+)"              # Clock name
                                      [^;]+;
                                      \s+([\d\.]+)\s+MHz     # Constraint
                                      \s+([\d\.]+)\s+MHz     # Actual
                                      \s+\d+                 # Level
                                      ''', re.VERBOSE | re.I)
        self.raw_title = ["clkName", "targetFmax", "fmax"]
        self.rpt_stop = re.compile("^$")  # empty line

    def get_title(self):
        return self.title

    def get_all_data(self):
        #return self.all_raw_data
        all_data = list()
        for item in self.all_raw_data:
            t_data = [item.get(foo) for foo in self.title]
            all_data.append(t_data)
        return all_data
    def set_all_data(self):
        self.all_raw_data = list()
        
            

    def get_data(self):
        if self.pap:
            return self.get_pap_data()
        else:
            return self.get_fmax_data()

    def get_pap_data(self):
        """The minimize PAP clock data
        """
        return self._get_minimize("PAP")

    def get_fmax_data(self):
        """The minimize fmax clock data
        """
        return self._get_minimize("fmax")

    def get_central_fmax(self):
        fmax_data = self._get_minimize("fmax")
        fmax_idx = self.title.index("fmax")
        return fmax_data[fmax_idx]

    def scan_report(self, rpt_file,  hot_clk=""):
        self.hot_clk = hot_clk
        self.rpt_file = rpt_file
        self.raw_data = list()
        if not os.path.isfile(rpt_file):
            return
        start, hotlines = 0, list()
        for line in open(rpt_file):
            if not start:
                if self.rpt_start.search(line):
                    start = 1
                continue
            line = line.strip()
            if self.rpt_stop.search(line):
                break
            line = re.sub("\s*\|", "", line)
            hotlines.append(line)
        hotlines = "".join(hotlines)

        self.raw_data  = self.rpt_pattern.findall(hotlines)
        if not self.raw_data:
            print_error("No frequency data found in %s" % rpt_file)
            return 1
            #
        
        ###########new add for score ###############
        for line in open(rpt_file):
            p = self.score.search(line)
            if p:
                self.score_data_setup = p.group(1)
                self.score_data_hold = p.group(2)
        self._get_all_raw_data(rpt_file)
           

    def _get_all_raw_data(self, rpt_file):
        self.all_raw_data = list()
        for item in self.raw_data:
            t_data = dict(zip(self.raw_title, item))
            full_path = os.path.abspath(rpt_file)
            full_path_re = re.compile(r'Target_(seed|fmax)_([\w\d_\.]+)')
            full_path_m = full_path_re.search(full_path)
            if full_path_m:
           
                try:
                    t_data["Seed/Target"]  = full_path_m.group(2)
                except:
                    t_data["Seed/Target"] = 'NA'
            else:
                t_data["Seed/Target"] = 'NA'
            t_data["twrLabel"] = os.path.basename(os.path.dirname( os.path.dirname( os.path.abspath(rpt_file) ) ) )+'_'+ \
                                os.path.basename( os.path.dirname( os.path.abspath(rpt_file) ) )+'_'+get_fname_ext(rpt_file)[0]
            clk_name = t_data.get("clkName")
            if self.hot_clk:
                if clk_name != self.hot_clk or clk_name.find(self.hot_clk)== -1:
                    continue
            lrl = self._get_lrl(t_data.get("clkName"))
            if lrl:
                t_data.update(lrl)
            pap_number = t_data.get("PAP")
            if not pap_number:
                pap_number = "%s/%s" % (t_data.get("fmax"), t_data.get("targetFmax"))
                pap_number = eval(pap_number) * 100
                pap_number = "%.2f%%" % pap_number
                t_data["PAP"] = pap_number
                t_data["PAPFactor"] = "-"
            ###########add for score ############
            t_data.update({'score_setup':self.score_data_setup,'score_hold':self.score_data_hold})
            ###########add for score ############
           
            self.all_raw_data.append(t_data)
            
            

    def _get_lrl(self, clk_name):
        start = 0
        for line in open(self.rpt_file):
            if not start:
                m_fre_clk = self.p_fre_clk.search(line)
                if m_fre_clk:
                    fre_clk = m_fre_clk.group(1)
                    if fre_clk == clk_name:
                        start = 1
                continue
            m_lrl = self.p_lrl.search(line)
            if m_lrl:
                lrl_title = ["logic", "route", "level"]
                lrl_data = [m_lrl.group(item) for item in lrl_title]
                return dict(zip(lrl_title, lrl_data))

    def _get_float(self, raw_str):
        raw_str = re.sub("\D+", "", raw_str)
        return float(raw_str)

    def _get_minimize(self, key):
        min_data = dict()
        for item in self.all_raw_data:
            new_value = item.get(key)
            old_value = min_data.get(key)
            if not old_value:
                min_data = item
            else:
                new_value = self._get_float(new_value)
                old_value = self._get_float(old_value)
                if old_value > new_value:
                    min_data = item
                    #return min_data
        return [min_data.get(item, "NA") for item in self.title]

class ScanLatticeMrp(ScanBasic):
    def __init__(self):
        ScanBasic.__init__(self)
        self.patterns = {
            "001 Version" : re.compile("^Mapper:.+version:\s+(.+)"),
            "002 Device" : [re.compile("^Target Device:\s+(\S+)"),
                            re.compile("^Target Performance:\s+(\S+)")],
            # Target Device:  LFEC6EFPBGA256
            # Target Performance:   5
            # Mapper:  ep5g00,  version:  Diamond Version 2.1.0.56
            # -----
            "003 Register" : [re.compile("^Number of registers:\s+(\d+)"),
                              re.compile("^Number of PFU registers:\s+(\d+)")],
            "004 Slice" : re.compile("^Number of SLICEs:\s+(\d+)"),
            "005 LUT" : [re.compile("^Total number of LUT4s:\s+(\d+)"),
                         re.compile("^Number of LUT4s:\s+(\d+)")],
            # Number of registers:    728
            # Number of SLICEs:           656 out of  3072 (21%)
            # Total number of LUT4s:     774
            #Number of LUT4s:        57172 out of 92016 
            # -----
            "006 IO" : [re.compile("^Number of PIO sites used:\s+(\d+)"),
                        re.compile("^Number of external PIOs:\s+(\d+)")],
            "007 EBR" : re.compile("^Number of block RAMs:\s+(\d+)"),
            "008 DRAM" : re.compile("^Number of distributed RAM:\s+(\d+)"),
            # Number of PIO sites used: 143 out of 195 (73%)
            # Number of block RAMs:  3 out of 10 (30%)
            # Number of distributed RAM:  16 (32 LUT4s)
            # -----
            "009 DSP" : re.compile("^Number of Used DSP Sites:\s+(\d+)"),
            "010 DSP_MULT" : re.compile("^Number of Used DSP MULT Sites:\s+(\d+)"),
            "011 DSP_ALU" : re.compile("^Number of Used DSP ALU Sites:\s+(\d+)"),
            # Number of Used DSP Sites:  0 out of 48 (0 %)
            # Number of Used DSP MULT Sites:  0 out of 48 (0 %)
            # Number of Used DSP ALU Sites:  0 out of 24 (0 %)
            # -----
            "012 PCS" : re.compile("^Number of PCS \(SerDes\):\s+(\d+)"),
            # Number of PCS (SerDes):  0 out of 1 (0%) with bonded PIO sites
            #Number of distributed RAM:   0 (0 LUT4s)
            "013 distributed_RAM" : re.compile("^Number of distributed RAM:\s+(\d+)"),
            #Number of ripple logic:    2598 (5196 LUT4s)
            "014 CCU" : re.compile("^Number of ripple logic:\s+(\d+)"),
            ### add dsp detail
            
            "015 MULT18X18D" : re.compile("^MULT18X18D\s+(\d+)"),
            "016 MULT9X9D" : re.compile("^MULT9X9D\s+(\d+)"),
            "017 ALU54B" : re.compile("^ALU54B\s+(\d+)"),
            "018 ALU24B" : re.compile("^ALU24B\s+(\d+)"),
            "019 PRADD18A" : re.compile("^PRADD18A\s+(\d+)"),
            "019 PRADD9A" : re.compile("^PRADD9A\s+(\d+)"),
            
            ###
            "stop" : re.compile("^Page\s+8"),

            "100 MapCPUTime" : re.compile("^Total CPU Time:\s+(.+)"),
            "101 MapPeakMem" : re.compile("^Peak Memory Usage:\s+(\d+) MB")
            # Total CPU Time: 0 secs
            # Peak Memory Usage: 51 MB
        }
        self.patterns_for_clocks={
                                  # Number of clocks:  1
                                  "001 clocks" : re.compile("^Number\s+of\s+clocks:\s+(\d+)"),
                                  #Net mclkin_c: 787 loads, 785 rising, 2 falling (Driver: PIO mclkin )
                                  "002 loads"  : re.compile("^Net\s+(.+):\s+(\d+)\s+loads"),
                                  "stop" : re.compile("^Page\s+8")
                                  }
        self.reset()
        self.data_clocks = {}
    def parse_line_clocks(self,mrp_file):  # 
        '''
           first: find the begin line 
           then scan the useful line
        '''
        lines = (file(mrp_file,'r')).readlines()
        #print lines
        begin_line_pattern =  self.patterns_for_clocks.get("001 clocks" )
        useful_line_pattern = self.patterns_for_clocks.get("002 loads" )
        begin =0
        clock_number = 0
        match_number = 0
        for line in lines:
            #print line
            line = line.strip()
            match_begin = begin_line_pattern.search(line)
            if  match_begin and begin==0:
                print line
                clock_number = int(match_begin.group(1))
                begin = 1
            elif begin == 1:
                match = useful_line_pattern.search(line)
                if match:
                    self.data_clocks[match.group(1)] = match.group(2)
                    match_number = match_number + 1
                    if match_number == clock_number:
                        break
                else:
                    pass
    def get_parse_line_clocks(self):
        return self.data_clocks
    
    def scan_clocks(self,mrp_file):
        self.keys_clocks = self.patterns_for_clocks.keys()
        self.data_clocks = {}
        line_count = get_file_line_count(mrp_file)
        stop_pattern = self.patterns_for_clocks.get("stop")
        if not line_count:
            return
        self.parse_line_clocks(mrp_file)

class ScanLatticeTime(ScanBasic):
    def __init__(self):
        ScanBasic.__init__(self)
        self.patterns = {
            "001 edif2ngd" : re.compile("^([\.\d]+)\s+,\s+\S+edif2ngd\s+"),
            "002 ngdbuild" : re.compile("^([\.\d]+)\s+,\s+\S+ngdbuild\s+"),
            "003 map" : re.compile("^([\.\d]+)\s+,\s+\S+map\s+"),
            "004 par" : re.compile("^([\.\d]+)\s+,\s+\S+par\s+"),
            "005 trce" : re.compile("^([\.\d]+)\s+,\s+\S+trce\s+")
        }
        self.patterns_srr = {
                             #Process took 0h:00m:01s realtime, 0h:00m:01s cputime
                             "001 srr_time" : re.compile("^Process\s+took\s+(.+)\s+realtime,\s+(.+)\s+cputime"),
                             #CCU2D:          88    #synthesis
                             #CCU2D => 85   #lse
                             "002 CCU_lse" : re.compile("^CCU.*?\s+(\d+)"),
                             #Peak Memory Usage: 125.008  MB
                             "003 SynPeakMem":re.compile("^Peak\sMemory\sUsage:\s+([\d.]+)"),
                             #Elapsed CPU time for LSE flow : 7.301  secs
                             "004 lse_CPU_Time":re.compile("^Elapsed\s+CPU\s+time\s+for\s+LSE\s+flow\s+:\s+([\d.]+)"),
                             "005 PostLSEClk_PostLSEFmax":re.compile('''create_clock\s+-period\s+\S+\s+-name\s+
                                        \S+\s+
                                        \[get_nets\s+(\S+?)\]\s+
                                        [\.\d]+\s+MHz\s+
                                        ([\.\d]+)\s+MHz''', re.VERBOSE | re.I),
                             #Peak Memory Usage: 51.152  MB
                             "007 LsePeakMem":re.compile("^Peak\sMemory\sUsage:\s+([\d.]+)"),
                             }
        self.patterns_mrp = {
                             #   Total CPU Time: 3 secs
                             "001 mrp_CPU_Time": re.compile("Total\s+CPU\s+Time:\s+(.+)"),

                             #   Total REAL Time: 4 secs
                             "002 mrp_REAL_Time": re.compile("Total\s+REAL\s+Time:\s+(.+)")
                             }
        self.patterns_par = {
                             #Total CPU time 1 mins 41 secs
                             "001 par_CPU_Time": re.compile("Total\s+CPU\s+time\s+to\s+completion:?\s+(.+)"), 
                             #Total REAL time: 1 mins 42 secs 
                             "002 par_REAL_Time": re.compile("Total\s+REAL\s+time\s+to\s+completion:\s+(.+)"),
                             "003 Complete" : re.compile("All signals are completely routed."),
                             "004 Par_Done" : re.compile("par done!"),
                             #PAR peak memory usage: 59.777 Mbytes
                             "005 ParPeakMem":re.compile("PAR\s+peak\s+memory\s+usage:\s+([\d \.]+)\s+Mbytes")

                             }
        self.srr_data     = {
                            'srr_Real_time':'NA',
                            'srr_Cpu_time' : 'NA',
                            'CCU_lse':'NA',
                            'SynPeakMem':'NA',
                            'lse_CPU_Time':'NA',
                            'PostLSEClk':'NA',
                            'PostLSEFmax':'NA',
                            'LsePeakMem':'NA'
                            }
        self.mrp_time = {}
        for key in self.patterns_mrp.keys():
            key = key.split()[1]
            self.mrp_time[key] = 'NA'
        self.par_data={}
        for key in self.patterns_par.keys():
            key = key.split()[1]
            self.par_data[key] = 'NA'
        self.reset()
    def scan_srr(self,srr_file):
        self.srr_data     = {
                            'srr_Real_time':'NA',
                            'srr_Cpu_time' : 'NA',
                            'CCU_lse':'NA',
                            'SynPeakMem':'NA',
                            'lse_CPU_Time':'NA',
                            'PostLSEClk':'NA',
                            'PostLSEFmax':'NA',
                            'LsePeakMem':'NA'
                            }
        if not srr_file:
            return 
        file_hand =  file(srr_file,'r')
        lines = file_hand.readlines()
        file_hand.close()
        hotlines=[]
        print srr_file
        raw_input()
        for line in lines:
            line = line.strip()
            hotlines.append(line)
            match = (self.patterns_srr["001 srr_time"]).search(line)
            if match:
                self.srr_data['srr_Real_time'] = time2secs(match.group(1))
                self.srr_data['srr_Cpu_time'] = time2secs(match.group(2))
                continue
            match = (self.patterns_srr["002 CCU_lse"]).search(line)
            if match:
                self.srr_data['CCU_lse'] = match.group(1)
            match = (self.patterns_srr["003 SynPeakMem"]).search(line)
            if match:
                self.srr_data['SynPeakMem'] = match.group(1)
            match = (self.patterns_srr["004 lse_CPU_Time"]).search(line)
            if match:
                self.srr_data['lse_CPU_Time'] = time2secs(match.group(1))
            
            match = (self.patterns_srr["007 LsePeakMem"]).search(line)
            if match and srr_file.endswith('synthesis.log'):
                self.srr_data['LsePeakMem'] = match.group(1)
                
        hotlines = " ".join(hotlines)
        hotlines = re.sub("\s*\|", "", hotlines)
        all_hot_data = self.patterns_srr["005 PostLSEClk_PostLSEFmax"].findall(hotlines)
        if not all_hot_data:
            pass
        else:
            old_fmax = -1
            for (clk_t, fmax_t) in all_hot_data:
                float_fmax = float(fmax_t)
                if old_fmax < 0 or float_fmax < old_fmax:
                    self.srr_data['PostLSEClk'] = clk_t
                    self.srr_data['PostLSEFmax'] = fmax_t
                    old_fmax = float(fmax_t)
        '''
            at here, we use to get the cpu time from the automat.log 
        '''
        automat_log = os.path.join(os.path.dirname(os.path.dirname(srr_file)),'run_till_map.log')
        if not os.path.isfile(automat_log):
            return
        elif srr_file.endswith('.srr'):
            temp_cpu_time = 0
            temp_real_time = 0
            set_flag = 0
            all_lines = file(automat_log).readlines()
            for l in all_lines:
                if l.startswith('edif2ngd:') or l.startswith('ngdbuild:'):
                    set_flag = 1
                value = re.compile("Total\s+CPU\s+Time:\s+(.+)")
                match = value.search(l)
                if match and set_flag:
                    match_data = match.group(1)
                    match_data = time2secs(match_data.strip())
                    temp_cpu_time += int(match_data)
                value = re.compile("Total\s+REAL\s+Time:\s+(.+)")
                match = value.search(l)
                if match and set_flag:
                    match_data = match.group(1)
                    match_data = time2secs(match_data.strip())
                    temp_real_time += int(match_data)
                    set_flag = 0
                    print temp_cpu_time
                    print temp_real_time
                    raw_input()
                if l.startswith('map'):
                    break
                
            try:
                self.srr_data['srr_Cpu_time'] =str( int(self.srr_data['srr_Cpu_time'])+int( temp_cpu_time) )
                self.srr_data['srr_Real_time'] =str( int(self.srr_data['srr_Real_time'])+int(temp_real_time) )
            except:
                print 'Error: in srr and automake add'
            
    def get_srr_time_data(self):
        return self.srr_data
    def scan_mrp(self,mrp_file):
        for key in self.patterns_mrp.keys():
            key = key.split()[1]
            self.mrp_time[key] = 'NA'
        if not mrp_file:
            return
        file_hand = file(mrp_file,'r')
        lines = file_hand.readlines()
        file_hand.close()
        
        
        for line in lines:
            line  = line.strip()
            for key in self.patterns_mrp.keys():
                value = self.patterns_mrp[key]
                match = value.search(line)
                if match:
                    print line
                    match_data = match.group(1)
                    match_data = time2secs(match_data.strip())
                    self.mrp_time[key.split()[1]] = match_data
    def get_mrp_time_data(self):
        return self.mrp_time    
    def scan_par(self,par_file):
        self.par_data={}
        for key in self.patterns_par.keys():
            key = key.split()[1]
            self.par_data[key] = 'NA'
        if not par_file:
            return
        file_hand = file(par_file,'r')
        lines = file_hand.readlines()
        file_hand.close()
        

        for line in lines:
            line = line.strip()
            for key in self.patterns_par.keys():
                p_value = self.patterns_par[key]
                p_match = p_value.search(line)
                p_key = key.split()[1]
                if p_match:
                    if p_key == 'Complete' or p_key == 'Par_Done':
                       self.par_data[p_key] = 'YES' 
                       print line
                    else:
                        self.par_data[p_key] = time2secs( (p_match.group(1)).strip() )            
    def get_par_time_data(self):
        return self.par_data
    def reset_par_time_data(self):
        for key in self.patterns_par.keys():
            key = key.split()[1]
            self.par_data[key] = 'NA'
    def get_total_time(self):
        srr_time = self.get_srr_time_data()
        mrp_time = self.get_mrp_time_data()
        par_time = self.get_par_time_data()
        self.total_time = {}
        self.real_time = 0
        self.cpu_time = 0
        for key in srr_time.keys():
            key_lower = key.lower()
            if key_lower.find('real_time')!= -1:
                try:
                    self.real_time = self.real_time + int(srr_time[key])
                except Exception,e:
                    print e
                    pass
            if key_lower.find('cpu_time')!= -1:
                try:
                    self.cpu_time = self.cpu_time + int(float(srr_time[key]))
                except:
                    pass
        for key in mrp_time.keys():
            key_lower = key.lower()
            if key_lower.find('real_time')!= -1:
                try:
                    self.real_time = self.real_time + int(mrp_time[key])
                except:
                    pass
            if key_lower.find('cpu_time')!= -1:
                try:
                    self.cpu_time = self.cpu_time +int( mrp_time[key])
                except:
                    pass
        for key in par_time.keys():
            key_lower = key.lower() 
            if key_lower.find('real_time')!= -1:
                try:
                    self.real_time = self.real_time + int(par_time[key])
                except:
                    pass
            if key_lower.find('cpu_time')!= -1:
                try:
                    self.cpu_time = self.cpu_time + int(par_time[key])
                except:
                    pass
        self.total_time['Total_cpu_time'] = str(self.cpu_time)
        self.total_time['Total_real_time'] = str(self.real_time)
        return self.total_time
    def get_title(self):
        srr_time = self.get_srr_time_data()
        mrp_time = self.get_mrp_time_data()
        par_time = self.get_par_time_data()
        srr_title =  srr_time.keys()
        srr_title.sort()
        mrp_title =  mrp_time.keys()
        mrp_title.sort()
        par_title =  par_time.keys()
        par_title.sort()
        title = srr_title+mrp_title+par_title+['Total_cpu_time','Total_real_time']  
        return title        
        
if __name__ == "__main__":
    mrp_file = r"D:\Users\yzhao1\workspace\scan_report\run\08_vj1kfpga\_xo2_synp\xo2_synp_08_vj1k.mrp"
    pp = ScanLatticeMrp()
    pp.scan_report(mrp_file)
    pp.scan_clocks(mrp_file)
    print pp.get_parse_line_clocks()
    print '----------------------'
    pp = ScanLatticeTime()
    pp.scan_srr(r'D:\Users\yzhao1\workspace\scan_report\bs\blowfish\_sc_synp\rev_1\sc_synp_blowfis.srr') 
    print pp.get_srr_time_data()
    pp.scan_mrp(mrp_file)
    print '-------------'
    pp.scan_par(r'\\d27817\test_dir\strdom_test_ecp3_ecp4u\zzz_ecp3_L25_job_dir_60standard\g64\_ecp3_synp\Target_Fmax_is_035MHz\5_1.par')
    print pp.get_par_time_data()
    pp.get_total_time()
    pp.get_title()