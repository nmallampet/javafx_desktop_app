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
    target_fmax_re = re.compile(r"Target_Seed_is_(.+)")
    if family:
        for_pattern = '_'+family
    if syn:
        for_pattern = for_pattern + '_'+syn
    dir_pattern = re.compile(for_pattern)
    
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
                    title = ["Design"] +['Target_seed']
                    #for item in scanner: #mrp twr time
                    title += (scanner[2]).get_title()
                    append_file(report_file_time, ",".join(title))
                report_file_clock = report_name+'_clock'+subtex
                append_file(report_file_clock, ",".join(['Design','Colck','Loads']))
        for design in all_designs:
            if need_design:
                if design == need_design:
                    pass
                else:
                    continue
            print_always( 'scanning %s'%design)
            design_path = os.path.join(job_dir,design)# e60_ecp3/g64
            srr_file = ''
            for dir in os.listdir(design_path): # dir:Target_Fmax_is_060MHz   
                dir_scan = os.path.join(design_path,dir)
                if (not re.search("Target", dir)) and os.path.isdir(dir_scan) and (not srr_file):
                    srr_file = get_unique_file([dir_scan, ".srr"])
                if re.search("Target", dir) and os.path.isdir(dir_scan):
                    pass
                else:
                    continue
                recover = RecoverPath(dir_scan)
                used_dir = ''
                for f_d in os.listdir(dir_scan):
                    f_d_full = os.path.join(dir_scan,f_d)
                    if os.path.isdir(f_d_full):
                        if used_dir:
                            print 'Worning: There are two implementation in the design'
                        used_dir = f_d_full
                
                if not srr_file:
                    pass
                else:
                    srr_file = os.path.join(design_path,dir,srr_file) 
                    scan_time.scan_srr(srr_file)
                mrp_file = get_unique_file([used_dir, ".mrp"])
                if not_exists(mrp_file, "map report file"):
                    
                    continue
                scan_mrp.scan_report(mrp_file)
                scan_time.scan_mrp(mrp_file)
                twr_file = time_file = par_file= ""
                target_fmax_for_time = '_'
                #------------------------------------------------#
                if 1:
                     useful_dir = used_dir
                     base_name = os.path.basename(useful_dir)
                     scan_time.reset_par_time_data()
                     target_fmax_for_match = target_fmax_re.search(dir)
                     if target_fmax_for_match:
                         target_fmax_for_time = target_fmax_for_match.group(1)
                     twr_p = os.path.join(useful_dir,'*'+base_name+".twr")
                     twr_file = get_unique_file(twr_p)
                     if not twr_file:
                         twr_file = get_unique_file([useful_dir, ".twr"])
                     time_file = os.path.join(useful_dir, time_file)
                     par_file = get_unique_file([useful_dir, ".par"])
                     if twr_file:
                         scan_twr.scan_report(twr_file)
                         scan_time.scan_report(time_file)
                         scan_time.scan_par(par_file)
                         #########################
                         #time_title = ['design']+scan_time.get_title2()
                         srr_data = scan_time.get_srr_time_data()
                         mrp_data = scan_time.get_mrp_time_data()
                         par_data = scan_time.get_par_time_data()
                         real_cpu_total = scan_time.get_total_time()
                         all_time_data = dict(srr_data.items()+mrp_data.items()+par_data.items() + real_cpu_total.items() )
                         data_list = []
                         for key in scan_time.get_title():
                             value = all_time_data.get(key,'NA')
                             data_list.append(value)
                         data_list2 = [design]+[target_fmax_for_time] +data_list
                         append_file(report_file_time,",".join(data_list2))
                             
                         #########################
                         #data = [design] + scan_mrp.get_data() + \
                         #      scan_twr.get_data() + data_list
                         if all_time_data['Complete'] == 'NA' or all_time_data['Par_Done'] == 'NA':
                             data_list = ['_']*len(scan_mrp.get_data()) +['_']*len(scan_twr.get_data()) +\
                                         ['_']*len(data_list)
                         else:
                             data_list = scan_mrp.get_data() + \
                               scan_twr.get_data() + data_list
                         data = [design] + data_list
                         append_file(report_file, ",".join(data))
                         data = [design]
                         append_file(report_file_mrp,",".join([design]+ scan_mrp.get_data() ))
                         append_file(report_file_twr,",".join([design]+ scan_twr.get_data() ))
                         #append_file(report_file_time,",".join([design]+ scan_time.get_data()))
                         scan_time.reset_par_time_data()
                     if not twr_file:
                         srr_data = scan_time.get_srr_time_data()
                         mrp_data = scan_time.get_mrp_time_data()
                         par_data = scan_time.get_par_time_data()
                         real_cpu_total = scan_time.get_total_time()
                         all_time_data = dict(srr_data.items()+mrp_data.items()+par_data.items() + \
                                               real_cpu_total.items() )
                         data_list = []
                         for key in scan_time.get_title():
                             value = all_time_data.get(key,'NA')
                             data_list.append(value)
                         data_list2 = [design]+[target_fmax_for_time] +data_list
                         append_file(report_file_time,",".join(data_list2))
                         #data = [design] + scan_mrp.get_data()+ data_list
                         if all_time_data['Complete'] == 'NA' or all_time_data['Par_Done'] == 'NA':
                             data = [design] + ['_']*len(scan_mrp.get_data())+  ['_']*len(data_list)
                         else:
                             data = [design] + scan_mrp.get_data()+ data_list
                         append_file(report_file, ",".join(data))
                         append_file(report_file_mrp,",".join([design]+ scan_mrp.get_data()))
                         scan_time.reset_par_time_data()
                scan_mrp.scan_clocks(mrp_file)
                clock_dict = scan_mrp.get_parse_line_clocks()
                for key in clock_dict.keys():
                    line = design+','+key+','+clock_dict[key]
                    append_file(report_file_clock,line)
                recover.run()
        os.chdir(root_dir)
    #for f in report_file_list:
    #    if os.path.isfile(f):
    #        file_sorted,note,design_fmax = report_sort.sort_csv(f)
    #        report_sort.write_note(file_sorted,note)
    #----------------update run_stand-------------------#
       
        
    
if __name__=='__main__':
    scan_report()