#! C:\Python25\python.exe
#===================================================
# Owner    : Yzhao1
# Function :
#           This file is used for scan_report for alter, this is just a scpecial for zhaojun
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
import tool_scan_altera_temp
from xlib import xFamily
from xOS import not_exists, wrap_copy_file, wrap_move_dir,get_unique_file,RecoverPath,get_unique_file
from xLog import print_always
from xTools import append_file
from xlib import report_sort_temp
def option():
    _family = xFamily.get_family_by_vendor('altera')
    _syn_dict = dict(lattice=("synp", "lse"), altera=("synp", "qis"), xilinx=("synp", "xst"), cube=("synp", "lse"))
    _synthesis = _syn_dict.get('altera') 
    public_group=optparse.OptionParser() 
    #public_group.add_option("--top-dir", help="specify top source path")
    public_group.add_option("--job-dir", default=os.getcwd(), help="specify job working path")
    #public_group.add_option("--tag", default="", help="specify tag name for results file")
    public_group.add_option("--synthesis", type="choice", default="synp", choices=_synthesis,
        help="specify synthesis name")
    #public_group.add_option("--design", help="specify design name")
    #public_group.add_option("--family", type="choice", choices=_family, help="specify family name")
    #public_group.add_option("--pap", action="store_true", help="dump Lattice Performance Achievement Percentage data")
    public_group.add_option("--report-name", default='report.csv',help="specify report_name you want to store")
    opt,args=public_group.parse_args() 
    return opt
def scan_report():
    opt = option()
    scanner = (tool_scan_altera_temp.ScanAlteraFit(), tool_scan_altera_temp.ScanAlteraFmax())
    job_dir = opt.job_dir
    #design = opt.design
    #syn = opt.synthesis
    #family = opt.family
    report_name = opt.report_name #this is useless now
    for_pattern=''
    #pap = opt.pap
    scan_fit = tool_scan_altera_temp.ScanAlteraFit()
    scan_fmax =  tool_scan_altera_temp.ScanAlteraFmax()
    scan_time_mem = tool_scan_altera_temp.ScanTimeMem()
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
            print_always( 'scanning %s'%design)
            design_path = os.path.join(job_dir,design)
            for dir in os.listdir(design_path): #at here dir should be as "_cyclone4_syn"
                dir_scan = os.path.join(design_path,dir)
                if os.path.isdir(dir_scan):
                    pass
                else:
                    continue
                if 1:
                    #####################
                    scanner = (scan_fit, scan_fmax,scan_time_mem)
                    report_file = os.path.join(job_dir,dir+'.csv')
                    report_file_list.append(report_file)
                    if not_exists(report_file):
                        title = ["Design"]
                        for item in scanner: #mrp twr time
                            title += item.get_title()
                        append_file(report_file, ",".join(title))
                    if 1:
                        report_name,subtex = os.path.splitext(report_file)
                        report_file_fit = report_name+'_fit'+subtex
                        if not_exists(report_file_fit):
                            title = ["Design"]
                            #for item in scanner: #mrp twr time
                            title += (scanner[0]).get_title()
                            append_file(report_file_fit, ",".join(title))
                        report_file_sta = report_name+'_sta'+subtex
                        if not_exists(report_file_sta):
                            title = ["Design"]
                            #for item in scanner: #mrp twr time
                            title += (scanner[1]).get_title()
                            append_file(report_file_sta, ",".join(title))
                        report_file_time_mem = report_name+'_time_mem'+subtex
                        if not_exists(report_file_time_mem):
                            title = ["Design"]
                            title += (scanner[2]).get_title()
                            append_file(report_file_time_mem, ",".join(title))
                        
                    ######################
                    recover = RecoverPath(dir_scan) 
                    fit_file = sta_file = map_file = srr_file =  ""
                    
                    if 'rev_1' in os.listdir('.'):
                        scan_time_mem.reset()
                        foo = os.path.join(dir_scan,'rev_1')
                        srr_file = get_unique_file(foo+'/'+'*.srr')
                        if srr_file:
                            scan_time_mem.scan_srr(srr_file)
                    
                    for foo in os.listdir("."):
                        data = []
                        if os.path.isdir(foo):
                            if re.search("Target", foo):
                                scan_fit.reset()
                                scan_fmax.reset()
                                scan_time_mem.set_fit_data()
                                scan_time_mem.set_map_data()
                                print_always("  Scanning %s" % foo)
                                fit_file = get_unique_file(foo+'/'+"*.fit.rpt")
                                sta_file = get_unique_file(foo+'/'+"*.sta.rpt")
                                map_file = get_unique_file(foo+'/'+'*.map.rpt')
                                if fit_file:
                                    scan_fit.scan_report(fit_file)
                                    scan_time_mem.scan_fit(fit_file)
                                    data1=[design]+scan_fit.get_data()
                                    append_file(report_file_fit, ",".join(data1))
                                    data = data + data1
                                if sta_file:
                                    scan_fmax.scan_report_temp(sta_file)
                                    data2=[design]+scan_fmax.get_data()
                                    append_file(report_file_sta, ",".join(data2))
                                    data = data + data2[1:]
                                if map_file:
                                    scan_time_mem.scan_map(map_file)
                                scan_t_m = scan_time_mem.get_data()
                                append_file(report_file_time_mem, ",".join([design]+scan_t_m))
                                data  = data + scan_t_m
                                append_file(report_file, ",".join(data))
                    recover.run()
            
            
            
            
        os.chdir(root_dir)               
    for f in report_file_list:
        if os.path.isfile(f):
            print '#######################################'
            file_sorted,note,design_fmax = report_sort_temp.sort_csv(f,col_key='PAP',add_average=1)
            report_sort_temp.write_note(file_sorted,note)

        
    
if __name__=='__main__':
    scan_report()