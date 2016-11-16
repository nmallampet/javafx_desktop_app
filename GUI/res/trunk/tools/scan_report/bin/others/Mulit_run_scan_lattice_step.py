#! C:\Python25\python.exe
#===================================================
# Owner    : Yzhao1
# Function :
#           This file is used for scan_report for lattice
#           
# Attention:
#          This is the second version. we try to run it independly
#          In this script, we try to scan it with mutil process
#           
# Date     :2014/2/12
#===================================================
from multiprocessing import Process,Lock,Array
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
        for design in all_designs:
            lock = Lock()
            if need_design:
                if design == need_design:
                    pass
                else:
                    continue
            def scan_sigal(job_dir,design):
                print_always( 'scanning %s'%design)
                design_path = os.path.join(job_dir,design)# e60_ecp3/g64
                srr_file = ''
                for dir in os.listdir(design_path): # dir:Target_Fmax_is_060MHz   
                    dir_scan = os.path.join(design_path,dir)
                    if (not re.search("Target", dir)) and os.path.isdir(dir_scan) and not srr_file:
                        srr_file = get_unique_file([dir_scan, ".srr"])
                for dir in os.listdir(design_path): # dir:Target_Fmax_is_060MHz   
                    dir_scan = os.path.join(design_path,dir)
                    #if (not re.search("Target", dir)) and os.path.isdir(dir_scan) and not srr_file:
                    #    srr_file = get_unique_file([dir_scan, ".srr"])
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
                    #srr_file = get_unique_file([used_dir, ".srr"])
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
                             lock.acquire()
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
                             #lock.release()
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
                             lock.acquire()
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
                    lock.release()
                    recover.run()
            Process(target=scan_sigal, args=(job_dir, design)).start()
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
        
    #----------------update run_stand-------------------#
    if 1:
        pass_log = glob.glob('pass_case.log')
        if pass_log:
            pass_log = pass_log[0]
        else:
            pass_log = '__'
        if os.path.isfile(pass_log):
            file_hand = file(pass_log,'r')
            lines = file_hand.readlines()
            file_hand.close()
            stand_name = glob.glob('*'+'_run_stand.bat')
            if stand_name:
                stand_name = stand_name[0]
            #stand_name = os.path.join(top_base,top_base2+'_'+syn+'_run_standard.bat')
            else:
                stand_name = ''
            run_standard = file(stand_name,'r')
            run_standard_lines = run_standard.readlines()
            run_standard_lines = list(set(run_standard_lines))
            run_standard.close()
            #--------------
            try :
                again_name = glob.glob('*'+'run_again.bat')
                if again_name:
                    again_name = again_name[0]
                #stand_name = os.path.join(top_base,top_base2+'_'+syn+'_run_standard.bat')
                else:
                    again_name = ''
                try:
                    again = file(again_name,'r')
                    again_lines = again.readlines()
                    again_lines = list(set(again_lines))
                    again.close()
                    replaced_job_dir = os.path.basename(os.getcwd())
                    again_lines = [re.sub(r'--job-dir=\S+','--job-dir='+replaced_job_dir,ag) for ag in again_lines]
                    new_again = []
                    #again_lines = [re.sub(r'--job-dir=\S+',replaced_job_dir,ag) for ag in again_lines]
                        #for ag in again_lines:
                    for ag in again_lines:
                            file_f = re.findall('--file=(\S+)',ag)
                            if file_f:
                                new_f = os.path.basename(file_f[-1])
                                ag = re.sub(r'--file=(\S+)','--file='+new_f,ag)
                            new_again.append(ag)
                            
                    run_standard_lines1 = run_standard_lines + new_again
                    run_standard_lines1 = list(set(run_standard_lines1))
                except:
                    run_standard_lines1 = list(set(run_standard_lines))
                    #pass
               
                #run_standard_lines = run_standard_lines + again_lines
            except:
                pass

                
            useful_lines = []
            for case in lines:
                case = case.strip()
                if not case:
                     continue
                else:
                     pass
                case_tab = '--design='+case
                case_tab_re = re.compile(case_tab+r'(\s+|$)')
                for line in run_standard_lines1:
                    line = line.strip()
                    if not line:
                        continue
                    if case_tab_re.search(line):
                        try:
                            fmax = float( design_fmax[case.strip()] )
                            fmax = str( int( fmax ))
                            line = re.sub(r'--fmax-sweep=[\s\d]+\d','',line)
                            line2 = line + ' --fmax-sweep='+fmax+' '+fmax+' '+'10 \n'
                            useful_lines.append(line2)
                        except:
                            #line2 = line
                            pass
                        
                        
            run_standard = file(stand_name,'w')
            run_standard.writelines(useful_lines)
            run_standard.close()
    
    if 1:  # write run_again.bat
        fail_log = glob.glob('fail_case.log')
        if fail_log:
            fail_log = fail_log[0]
        else:
            fail_log = '__'
        if os.path.isfile(fail_log):
            file_hand = file(fail_log,'r')
            lines = file_hand.readlines()
            lines = list(set(lines))
            file_hand.close()
            if 1:
                if not run_standard_lines:
                    stand_name = glob.glob('*'+'_run_stand.bat')
                    if stand_name:
                        stand_name = stand_name[0]
                    #stand_name = os.path.join(top_base,top_base2+'_'+syn+'_run_standard.bat')
                    else:
                        stand_name = ''
                    try:
                        run_standard = file(stand_name,'r')
                        run_standard_lines = run_standard.readlines()
                        run_standard_lines = list(set(run_standard_lines))
                        run_standard.close()
                    except:
                        run_standard_lines = []
                replaced_job_dir = str(os.path.abspath(os.getcwd()))
                replaced_job_dir = re.sub(r'\\','/',replaced_job_dir)
                run_standard_lines = [re.sub(r'--job-dir=\S+',r'--job-dir='+replaced_job_dir,ag) for ag in run_standard_lines]
                new_again = []
                for ag in run_standard_lines:
                        file_f = re.findall('--file=(\S+)',ag)
                        if file_f:
                            new_f = '--file='+'../'+file_f[-1]
                            ag = re.sub(r'--file=(\S+)',new_f,ag)
                        new_again.append(ag)
                run_standard_lines = new_again
                try:
                    again_name = glob.glob('*'+'run_again.bat')
                    if again_name:
                        again_name = again_name[0]
                    #stand_name = os.path.join(top_base,top_base2+'_'+syn+'_run_standard.bat')
                    else:
                        again_name = ''
                    again = file(again_name,'r')
                    again_lines = again.readlines()
                    again_lines = list(set(again_lines))
                    again.close()
                    run_standard_lines = run_standard_lines + again_lines
                except:
                    pass
            useful_lines = []
            for case in lines:
                case = case.strip()
                if not case:
                     continue
                else:
                     pass
                case_tab = '--design='+case
                case_tab_re = re.compile(case_tab+r'(\s+|$)')
                for line in run_standard_lines:
                    line = line.strip()
                    if not line:
                        continue
                    if case_tab_re.search(line):

                        try:
                            line2 = line+'\n'
                            useful_lines.append(line2)
                            #fmax = float( design_fmax[case.strip()] )
                            #fmax = str( int( fmax ))
                            #line = case_tab_re.sub('',line)
                            #line = re.sub(r'--fmax-sweep=[\s\d]+\d','',line)
                            #line2 = line + ' --fmax-sweep='+fmax+' '+fmax+' '+'10 \n'
                        except:
                            pass
                            #pass
                        
                        
            run_standard = file('run_again.bat','w')
            run_standard.writelines(useful_lines)
            run_standard.close()
         
        
    
if __name__=='__main__':
    scan_report()