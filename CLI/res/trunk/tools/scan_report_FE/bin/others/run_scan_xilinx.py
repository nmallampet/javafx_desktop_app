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
import re,glob
scan_report_dir = os.path.join(os.path.dirname(__file__),'..','tools','scan_tools')
scan_report_dir = os.path.abspath(scan_report_dir)
if scan_report_dir in sys.path:
    pass
else:
    sys.path.append(scan_report_dir)
import tool_scan_xilinx
from xlib import xFamily
from xOS import not_exists, wrap_copy_file, wrap_move_dir,get_unique_file,RecoverPath,get_unique_file
from xLog import print_always
from xTools import append_file
from xlib import report_sort
def option():
    _family = xFamily.get_family_by_vendor('xilinx')
    _syn_dict = dict(lattice=("synp", "lse"), altera=("synp", "qis"), xilinx=("synp", "xst"), cube=("synp", "lse"))
    _synthesis = _syn_dict.get('xilinx') 
    public_group=optparse.OptionParser() 
    public_group.add_option("--top-dir", help="specify top source path")
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
    design = opt.design
    syn = opt.synthesis
    family = opt.family
    report_name = opt.report_name #this is useless now
    for_pattern=''
    pap = opt.pap
    scan_mrp = tool_scan_xilinx.ScanXilinxMrp()
    scan_twr = tool_scan_xilinx.ScanXilinxTwr()
    scan_time = tool_scan_xilinx.ScanXilinxTimeMem()
    if family:
        for_pattern = '_'+family
    if syn:
        for_pattern = for_pattern + '_'+syn
    dir_pattern = re.compile(for_pattern)
    print for_pattern
    
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
    if design:
        print_always( 'scanning %s'%design)
        if design in all_designs:
            design_path = os.path.join(job_dir,design)
            for dir in os.listdir(design_path):
                if dir_pattern.search(dir) and os.path.isdir(os.path.join(design_path,dir)):
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
                        
                ######################
                    dir_scan = os.path.join(design_path,dir)
                    twr_file = time_file = ""
                    if 'rev_1' in os.listdir(dir_scan):
                        scan_time.set_srr_data()
                        foo = os.path.join(dir_scan,'rev_1','*.srr')
                        srr_file = get_unique_file(foo)
                        if srr_file:
                            scan_time.scan_srr(srr_file)  
                    for foo in os.listdir(dir_scan):
                        foo =  os.path.join(dir_scan,foo)
                        if os.path.isdir(foo):
                            if re.search("Target", foo):
                                print foo
                                recover = RecoverPath( os.path.join(design_path,dir,foo) )
                                scan_mrp.reset()
                                #scan_time.reset()
                                scan_time.set_map_data()
                                scan_time.set_par_data()
                                
                                scan_twr.reset()
                                print_always("  Scanning %s" % foo)
                                _project_name = "%s_%s" % (dir, design[:7])
                                project_name = _project_name.strip("_")
                                mrp_file = os.path.join(dir_scan,foo,project_name + "_map.mrp")
                                scan_mrp.scan_report(mrp_file)
                                scan_time.scan_map(mrp_file)
                                par_file = os.path.join(dir_scan,foo,project_name + ".par")
                                scan_time.scan_par(par_file)
                                twr_file = os.path.join(dir_scan,foo,project_name + ".twr")
                                
                                scan_twr.scan_report(twr_file)
                                time_file = os.path.join(dir_scan,foo,"mpar_log.time")
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
                                

                
        else:
            print 'The design is not exists in the job_dir'
            return
    else:
        root_dir = os.getcwd()
        for design in all_designs:
            print_always( 'scanning %s'%design)
            design_path = os.path.join(job_dir,design)
            for dir in os.listdir(design_path):
                if dir_pattern.search(dir) and os.path.isdir(os.path.join(design_path,dir)):
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
                        
                ######################
                    dir_scan = os.path.join(design_path,dir)
                    twr_file = time_file = ""
                    if 'rev_1' in os.listdir(dir_scan):
                        scan_time.set_srr_data()
                        foo = os.path.join(dir_scan,'rev_1','*.srr')
                        srr_file = get_unique_file(foo)
                        if srr_file:
                            scan_time.scan_srr(srr_file)
                            
                    for foo in os.listdir(dir_scan):
                        if os.path.isdir( os.path.join(design_path,dir,foo) ):
                            if re.search("Target", foo):
                                recover = RecoverPath(os.path.join(design_path,dir,foo))
                                scan_mrp.reset()
                                #scan_time.reset()
                                scan_time.set_map_data()
                                scan_time.set_par_data()
                                scan_twr.reset()
                                print_always("  Scanning %s" % foo)
                                _project_name = "%s_%s" % (dir, design[:7])
                                project_name = _project_name.strip("_")
                                mrp_file = os.path.join(dir_scan,foo,project_name + "_map.mrp")
                                scan_mrp.scan_report(mrp_file)
                                scan_time.scan_map(mrp_file)
                                par_file = os.path.join(dir_scan,foo,project_name + ".par")
                                scan_time.scan_par(par_file)
                                twr_file = os.path.join(dir_scan,foo,project_name + ".twr")
                                scan_twr.scan_report(twr_file)
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
            file_sorted,note,design_fmax = report_sort.sort_csv(f)
            report_sort.write_note(file_sorted,note)
    #----------------update run_stand-------------------#
    pass_log = glob.glob('*'+syn+'_pass_case.log')
    if pass_log:
        pass_log = pass_log[0]
    else:
        pass_log = '__'
    if os.path.isfile(pass_log):
        file_hand = file(pass_log,'r')
        lines = file_hand.readlines()
        file_hand.close()
        stand_name = glob.glob('*'+syn+'_run_standard.bat')
        if stand_name:
            stand_name = stand_name[0]
        #stand_name = os.path.join(top_base,top_base2+'_'+syn+'_run_standard.bat')
        else:
            stand_name = ''
        run_standard = file(stand_name,'r')
        run_standard_lines = run_standard.readlines()
        run_standard.close()
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
                        fmax = float( design_fmax[case.strip()] )
                        fmax = str( int( fmax ))
                    #line = case_tab_re.sub('',line)
                        line = re.sub(r'--fmax-sweep=[\s\d]+\d','',line)
                        line2 = line + ' --fmax-sweep='+fmax+' '+fmax+' '+'10 \n'
                    except:
                        line2 = line
                    
                    useful_lines.append(line2)
        run_standard = file(stand_name,'w')
        run_standard.writelines(useful_lines)
        run_standard.close()
        '''        
        design_re = re.compile('--design=\S+')
        job_dir_re = re.compile('--job-dir=\S+')
        space_re = re.compile('\s+')
        cmd = design_re.sub('',cmd)
        cmd = job_dir_re.sub('',cmd)
        cmd = space_re.sub(' ',cmd)
        for line in lines:
            cmd_again =  'python  '+cmd + '  --design='+line.strip() +'  --job-dir='+top_base2
            if line.strip() in design_fmax.keys():
                try:
                    fmax = float( design_fmax[line.strip()] )
                    fmax = str( int( fmax ))
                    cmd_standard = cmd_again + ' --fmax-sweep='+fmax+' '+fmax+' '+'10'
                except Exception,e:
                    print e
                    cmd_standard = cmd_again
                run_standard.write(cmd_standard+'\n')
        run_standard.close()'''
    ''' 
    fail_log = glob.glob('*'+syn+'_fail_case.log')
    try:
        fail_log = fail_log[0]
    except:
        print 'can not find fail_log file'
        return 
    if os.path.isfile(fail_log):
        file_hand = file(fail_log,'r')
        lines = file_hand.readlines()
        #run_again = file(os.path.join(top_base,top_base2+'_'+syn+'_run_again.bat'),'w')
        again_name = glob.glob('*'+syn+'_run_again_lines.txt')
        if again_name:
            again_name = again_name[0]
        #stand_name = os.path.join(top_base,top_base2+'_'+syn+'_run_standard.bat')
        else:
            again_name = ''
        try:
            run_again = file(again_name,'r')
        except:
            return
        run_again_lines = run_again.readlines()
        run_again.close()
        again_name = again_name.replace('_run_again_lines.txt','_run_again.bat')
        run_again = file(again_name,'w')
        used_case = []
        for line in lines:
            line = line.split(':')[0]
            line = line.strip()
            if line in used_case :
                continue
            for again_line in run_again_lines:
                if again_line.find('--design='+line)!= -1:
                    run_again.write(again_line+'\n')
            #used_case.append(line)
            #cmd_again =  'python  '+cmd + '  --design='+line.strip() +' --job-dir='+'../'+top_base2
            #run_again.write(cmd_again+'\n')
        run_again.close()'''
        
    
if __name__=='__main__':
    scan_report()
    