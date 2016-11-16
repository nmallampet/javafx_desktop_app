#! C:\Python25\python.exe
#===================================================
# Owner    : Yzhao1
# Function :
#           This file is used for scan_report for lattice,just a temp file for Zhaojun!!!
#           
# Attention:
#           
#           
# Date     :2013/2/5
#===================================================
import optparse
import os
import sys
import re,glob
scan_report_dir = os.path.join(os.path.dirname(__file__),'..','tools','scan_tools')
scan_report_dir = os.path.abspath(scan_report_dir)
if scan_report_dir in sys.path:
    pass
else:
    sys.path.append(scan_report_dir)
import tool_scan_xilinx_temp
from xlib import xFamily
from xOS import not_exists, wrap_copy_file, wrap_move_dir,get_unique_file,RecoverPath,get_unique_file
from xLog import print_always
from xTools import append_file
from xlib import report_sort_temp
def option():
    _family = xFamily.get_family_by_vendor('xilinx')
    _syn_dict = dict(lattice=("synp", "lse"), altera=("synp", "qis"), xilinx=("synp", "xst"), cube=("synp", "lse"))
    _synthesis = _syn_dict.get('xilinx') 
    public_group=optparse.OptionParser() 
    #public_group.add_option("--top-dir", help="specify top source path")
    public_group.add_option("--job-dir", default=os.getcwd(), help="specify job working path")
    #public_group.add_option("--tag", default="", help="specify tag name for results file")
    public_group.add_option("--synthesis", type="choice", default="synp", choices=_synthesis,
        help="specify synthesis name")
    #public_group.add_option("--design", help="specify design name")
    #public_group.add_option("--family", type="choice", choices=_family, help="specify family name")
    public_group.add_option("--pap", action="store_true", help="dump Lattice Performance Achievement Percentage data")
    public_group.add_option("--report-name", default='report.csv',help="specify report_name you want to store")
    opt,args=public_group.parse_args() 
    return opt
def scan_report():
    opt = option()
    job_dir = opt.job_dir
    report_name = opt.report_name #this is useless now
    scan_mrp = tool_scan_xilinx_temp.ScanXilinxMrp()
    scan_twr = tool_scan_xilinx_temp.ScanXilinxTwr()
    scan_time = tool_scan_xilinx_temp.ScanXilinxTimeMem()


    
    all_designs = []
    if os.path.isdir(job_dir):
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
    if 0:
        pass
    else:
        root_dir = os.getcwd()
        for design in all_designs:
            design_path = os.path.join(job_dir,design)
            for dir in os.listdir(design_path):
                if  os.path.isdir(os.path.join(design_path,dir)):
                    #####################
                    scanner = (scan_mrp, scan_twr, scan_time)
                    report_file = os.path.join(job_dir,dir+'.csv')
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
                        report_file_time = report_name+'_time_mem'+subtex
                        if not_exists(report_file_time):
                            title = ["Design"]
                            #for item in scanner: #mrp twr time
                            title += (scanner[2]).get_title()
                            append_file(report_file_time, ",".join(title))
                    for dir2 in os.listdir( os.path.join(design_path,dir) ):
                        dir_scan = os.path.join(design_path,dir,dir2)
                        if os.path.isdir(dir_scan):
                            pass
                        else:
                            continue
                        print dir_scan
                        twr_file = time_file = ""
                        if 'rev_1' in os.listdir( os.path.join(design_path,dir)):
                            scan_time.set_srr_data()
                            foo = os.path.join(dir_scan,'*.srr')
                            srr_file = get_unique_file(foo)
                            if srr_file:
                                scan_time.scan_srr(srr_file)
                                
                        if 1:
                            foo = dir2
                            if 1:
                                if re.search("Target", foo):
                                    recover = RecoverPath(dir_scan)
                                    scan_mrp.reset()
                                    #scan_time.reset()
                                    scan_time.set_map_data()
                                    scan_time.set_par_data()
                                    scan_twr.reset()
                                    print_always("  Scanning %s" % foo)
                                    _project_name = "%s_%s" % (dir, design[:7])
                                    project_name = _project_name.strip("_")
                                    mrp_file = os.path.join(dir_scan,project_name + "_map.mrp")
                                    scan_mrp.scan_report(mrp_file)
                                    scan_time.scan_map(mrp_file)
                                    par_file = os.path.join(dir_scan,project_name + ".par")
                                    scan_time.scan_par(par_file)
                                    twr_file = os.path.join(dir_scan,project_name + ".twr")
                                    scan_twr.scan_report_temp(twr_file)
                                    #time_file = os.path.join(dir_scan,foo,"mpar_log.time")
                                    #scan_time.scan_report(time_file)
                                    data1 = scan_mrp.get_data()
                                    append_file(report_file_mrp, ",".join([design]+data1))
                                    data2 = scan_twr.get_data()
                                    append_file(report_file_twr, ",".join([design]+data2))
                                    data3 = scan_time.get_data()
                                    append_file(report_file_time, ",".join([design]+data3))
                                    data = data1 + data2+ data3
                                    append_file(report_file, ",".join([design]+data))
                                    recover.run()
        
            
        os.chdir(root_dir)
    for f in report_file_list:
        if os.path.isfile(f):
            file_sorted,note,design_fmax = report_sort_temp.sort_csv(f,col_key='PAP',add_average=1)
            report_sort_temp.write_note(file_sorted,note)
    
if __name__=='__main__':
    scan_report()
    