#! C:\Python25\python.exe
#===================================================
# Owner    : Yzhao1
# Function :
#           This file is used for scan_report for lattice
#           
# Attention:
#           
#           
# Date     :2013/2/5
#===================================================
import optparse
import os
import sys
import re
import glob
scan_report_dir = os.path.join(os.path.dirname(__file__),'..','tools','scan_tools')
scan_report_dir = os.path.abspath(scan_report_dir)
if scan_report_dir in sys.path:
    pass
else:
    sys.path.append(scan_report_dir)
import tool_scan_lattice
from xlib import xFamily
from xOS import not_exists, wrap_copy_file, wrap_move_dir,get_unique_file,RecoverPath
from xLog import print_always
from xTools import append_file
from xlib import report_sort
def option():
    _family = xFamily.get_family_by_vendor('lattice')
    _syn_dict = dict(lattice=("synp", "lse"), altera=("synp", "qis"), xilinx=("synp", "xst"), cube=("synp", "lse"))
    _synthesis = _syn_dict.get('lattice') 
    public_group=optparse.OptionParser() 
    #public_group.add_option("--top-dir", help="specify top source path")
    public_group.add_option("--job-dir", default=os.getcwd(), help="specify job working path")
    #public_group.add_option("--tag", default="", help="specify tag name for results file")
    public_group.add_option("--synthesis", type="choice", default="synp", choices=_synthesis,
        help="specify synthesis name")
    public_group.add_option("--design", help="specify design name")
    public_group.add_option("--family", type="choice", choices=_family, help="specify family name")
    public_group.add_option("--pap", action="store_true", help="dump Lattice Performance Achievement Percentage data")
    public_group.add_option("--report-name", default='report.csv',help="specify report_name you want to store")
    opt,args=public_group.parse_args() 
    return opt
def scan_report():
    opt = option()
    job_dir = opt.job_dir
    job_dir = os.path.abspath(job_dir)
    need_design = opt.design
    syn = opt.synthesis
    #syn = ''
    family = opt.family
    report_name = opt.report_name #this is useless now
    for_pattern=''
    pap = opt.pap
    scan_mrp = tool_scan_lattice.ScanLatticeMrp()
    scan_twr = tool_scan_lattice.ScanLatticeTwr(pap)
    scan_time = tool_scan_lattice.ScanLatticeTime()
    target_fmax_re = re.compile(r"Target_Fmax_is_(.+)MHz")
    if family:
        for_pattern = '_'+family
    if syn:
        for_pattern = for_pattern + '_'+syn
    dir_pattern = re.compile(for_pattern)
    
    all_designs = []
    if os.path.isdir(job_dir):
        for d in os.listdir(job_dir):
            if d.endswith('.csv'):
                d = os.path.join(job_dir,d)
                os.remove(d)
        pass
    else:
        print 'The job_dir:%s is not a directory'%job_dir
        return
    for dir in os.listdir(job_dir):  # get all the design path
        dir2 =  os.path.join(job_dir,dir)
        if os.path.isdir(dir2):
            all_designs.append(dir)
        else:
            pass
    report_file_list = []

    
    if 1:
        root_dir = os.getcwd()
        dir = os.path.basename(opt.job_dir)
        if 1:
            scanner = (scan_mrp, scan_twr, scan_time)
            report_file = os.path.join(opt.job_dir,dir+'.csv')
            if report_file in report_file_list:
                pass
            else:
                report_file_list.append(report_file)
            if not_exists(report_file):
                title = ["Design"]
                for item in scanner: #mrp twr time
                    title += item.get_title()
                append_file(report_file, ",".join(title))
            if 1:
                report_name,subtex = os.path.splitext(report_file)
                report_file_mrp = report_name+'_mrp'+subtex
                if not_exists(report_file_mrp):
                    title = ["Design"]
                    #for item in scanner: #mrp twr time
                    title += (scanner[0]).get_title()
                    append_file(report_file_mrp, ",".join(title))
                report_file_twr = report_name+'_twr'+subtex
                if not_exists(report_file_twr):
                    title = ["Design"]
                    #for item in scanner: #mrp twr time
                    title += (scanner[1]).get_title()
                    append_file(report_file_twr, ",".join(title))
                report_file_time = report_name+'_time'+subtex
                if not_exists(report_file_time):
                    title = ["Design"] +['Target_fmax']
                    #for item in scanner: #mrp twr time
                    title += (scanner[2]).get_title()
                    append_file(report_file_time, ",".join(title))
                report_file_clock = report_name+'_clock'+subtex
                append_file(report_file_clock, ",".join(['Design','Colck','Loads']))
        #for design in all_designs:
        for root1,dirs1,files1 in os.walk(job_dir):
            root1 = os.path.abspath(root1)
            find_mrp = 0;
            for f in files1:
                    if f.endswith('.mrp'):
                        find_mrp = 1
                        break
            if find_mrp == 1:
                pass
            else:
                continue
            design_path = os.path.join(root1)# e60_ecp3/g64
            srr_file = ''
            for dir in os.listdir(design_path): # dir:Target_Fmax_is_060MHz   
                dir_scan = os.path.join(design_path,dir)
                if (not re.search("Target", dir)) and os.path.isdir(dir_scan) and not srr_file:
                    srr_file = get_unique_file([dir_scan, ".srr"])
            for dir in os.listdir(design_path): # dir:Target_Fmax_is_060MHz   
                
                #if (not re.search("Target", dir)) and os.path.isdir(dir_scan) and not srr_file
                recover = RecoverPath(design_path)
                
                mrp_file = get_unique_file([design_path, ".mrp"])
                if not_exists(mrp_file, "map report file"):
                    
                    continue
                scan_mrp.scan_report(mrp_file)
                scan_time.scan_mrp(mrp_file)
                append_file(report_file_mrp,",".join([design_path]+ scan_mrp.get_data()))
                recover.run()
        os.chdir(root_dir)
    for f in report_file_list:
        if os.path.isfile(f):
            file_sorted,note,design_fmax = report_sort.sort_csv(f)
            report_sort.write_note(file_sorted,note)
    file_hand_pass = file('pass_case.log','w')
    #----------read fail case first:
    try:
        file_hand_fail = file('fail_case.log','r')
        fail_case_lines = file_hand_fail.readlines()
        file_hand_fail.close()
        fail_case_lines2 = [f_c.split(':')[0].strip() for f_c in fail_case_lines]
    except:
        fail_case_lines2 = []
    file_hand_fail = file('fail_case.log','w')
    for key in design_fmax.keys():
        v = design_fmax[key]
        
        if re.search(r'\d',v):
            file_hand_pass.write(key+'\n')
        else:
            if key in fail_case_lines2:
                fail_k = fail_case_lines[fail_case_lines2.index(key)]
                file_hand_fail.write(fail_k.strip()+'\n')
            else:
                file_hand_fail.write(key+'\n')
    file_hand_pass.close()
    file_hand_fail.close()
        
    
        
        
    
if __name__=='__main__':
    scan_report()