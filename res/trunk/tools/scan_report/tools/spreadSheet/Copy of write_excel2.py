#! C:\Python25\python.exe
#coding:utf-8
#===================================================
# Owner    : Yzhao1
# Function :
#           This file used to write the compared data from compared csv file
# Attention:
#           
# Date     :2013/1/10

#===================================================
import os,sys,csv
import re
import csv_compare
from pyExcelerator import *
import style
import traceback
import optparse
import string
xlib = os.path.join(os.path.dirname(__file__),'..','..','bin','xlib')
xlib = os.path.abspath(xlib)
if xlib in sys.path:
    pass
else:
    sys.path.append(xlib)
from xConf import get_conf_options
red_range = 0.05
red_range_dic  = get_conf_options('param.conf')['red_range']
case_list_path =  os.path.join(__file__,'..','..','..','conf','case_list.conf')
case_list_path = os.path.abspath(case_list_path)
case_group_dic = get_conf_options(case_list_path)['case_list']
group_cases= {}
for group,cases in case_group_dic.items():
    cases = cases.split()
    for item in cases:
        group_cases.setdefault(group,[]).append(item)
all_groups = group_cases.keys()
statistic_tie_list = [['win','win'],
        ['tie','tie'],
        ['lose','lose'],
        ['less15','<-0.15'],
        ['less15_5','-0.15~-0.05'],
        ['less5_5','-0.05~-0.05'],
        ['more5_15','0.05~-0.15'],
        ['more15','>0.15']]
key_order = ['Fmax','Regs','Luts','Slices','Runtime','MapPMem','ParPMem']

def gen_dict():
    '''
       This function used to return the col dictionary in the excel as:
       A,B,---,AA,---ZZ
    '''
    all_letter = string.ascii_uppercase
    result = {}
    list_num_one = [ a_letter for a_letter in all_letter ]
    list_num_two = [ i + j for i in all_letter for j in all_letter]
    for key,value  in enumerate(list_num_one + list_num_two ): 
        result.setdefault(key,value)
        #print key+1,'==>',value
    return result
def write_note(note={},totalSheet=''):
    dict_return = note
    row = 0
    col = 0
    totalSheet.col(0).width = 3*3328/10
    key_style = style.myCreateStyle(myBorders='4444',myPattern='yellow' )
    for key,value in dict_return.items():
        if len(key)<= 5:
            length = 1
            totalSheet.write(row, 1, '['+key+']',key_style)
        else:
            length = len(key)/9 +1
            totalSheet.write_merge(row, row, 1, length,'['+key+']',key_style)
        row = row + 1
        col = length+1
        for id1,item in enumerate(value):
            
            if id1 == 0:
                if len(item)<= 5:
                    totalSheet.write(row-1, col, item)
                else:
                    length = len(item)/9 + 1
                    totalSheet.write_merge(row-1, row-1, col, length+col,item)
            else:
                '''if len(key)<= 5:
                    totalSheet.write(row, col-1, '')
                else:
                    length = len(key)/9
                    totalSheet.write_merge(row, row, col-1, length,'')'''
                if len(item)<= 5:
                    totalSheet.write(row, col, item)
                else:
                    length = len(item)/9
                    totalSheet.write_merge(row, row, col, length+col,item)
                row = row + 1
    return row
def write_title(title=[],totalSheet='',row=0):
    style_center = style.myCreateStyle(myBorders='4222',myAlignment='010')
    style_center.font.bold = 'True'
    totalSheet.write(row, 0, 'ID',style_center)
    col = 1
    for item in title:
        totalSheet.write(row, col, item,style_center)    
        col = col + 1
def write_file_name(line='',totalSheet='',row=0):
    #find_comma = re.compile(r'(,[^,])')
    style_center = style.myCreateStyle(myBorders='2422',myAlignment='010' )
    line = line.split(',')
    finder = 0
    begin = 0
    end = 0
    totalSheet.write(row,0, '',style_center)
    totalSheet.write(row,1, '',style_center)
    for id1,item in enumerate(line):
        if item and finder ==0:
            finder = 1
            content = item
            begin = 2
            continue
        if item and finder ==1:
            end = id1
            totalSheet.write_merge(row,row,begin,end, content,style_center)
            #print content,begin,end
            begin = id1+1
            content = item
            finder = 1
            continue
        if id1 == len(line)-1:
            cols = len(line) -  begin
            totalSheet.write_merge(row,row,begin,len(line)+1, content,style_center)    
def read_csv_note(csv_file):
    ''' This function used to read the data from csv file
        At here the csv file should can contain note  before case data
        
        After the note lines should be the title as "Design,.....":
            asdfasfasdfasdfasdfas     |
            fd                        |  
            df                        |
            ffea                      |   these are the note lines
            Desing,Lut.......
            case1,data1,.....
            ......
        the return data is a dictionary
        dict[desing] = line1
        
        And if there is summary part:
        the summary part should begin with 'Sumary:'
    '''
    csv_dic = {}
    note= []
    begin = 0
    hot_lines = []
    sumary_lines = []
    sumary_begin = 0
    for id1,line in enumerate(open(csv_file)):
            line = line.strip()
            if begin == 0:
                if line.startswith('Design'):
                    begin = 1
                    hot_lines.append(line) # this is the title
                    title = line
                    continue
                else:
                    note.append(line)
            elif line.startswith('Sumary'):
                 sumary_lines.append(line)
                 sumary_begin = 1
            elif sumary_begin ==1:
                sumary_lines.append(line)
            else:
                hot_lines.append(line) 
    file_hand = file(csv_file,'rb')
    ori_dict = csv.DictReader(hot_lines)
    for item in ori_dict:
        design = item.get('Design','None')
        if design != 'None' and design != '':
            csv_dic[design] = item
        else:
            pass
        #print item
    title = ori_dict.fieldnames
    return [csv_dic,title,note,sumary_lines]        
def write_body(csv_dic={},title=[],totalSheet='',row=1):
    
    statistic_style = style.myCreateStyle(myBorders='2222' )
    statistic_percent = style.myCreateStyle(myBorders='2222',myFont='0.00%' )
    body_style = style.myCreateStyle(myBorders='4444' )
    body_style_red = style.myCreateStyle(myBorders='4444',myFont='red' )
    body_style_green = style.myCreateStyle(myBorders='4444',myFont='green' )
    body_style_left = style.myCreateStyle(myBorders='4424' )
    body_style_left_red = style.myCreateStyle(myBorders='4424',myFont='red' )
    body_style_left_green = style.myCreateStyle(myBorders='4424',myFont='green' )
    body_style_right = style.myCreateStyle(myBorders='4442' )
    title_col_dic= {} # after process, it will be: title:[valu1,value2,valu3.....]
    excel_col_names = gen_dict()
    case_line_begin = row
    cases = csv_dic.keys()
    cases.sort()
    col_names = {}
    row_names = {}
    case_id = 0
    select_group_list=[]
    
    for id1,case in enumerate(cases):  # at here, we demand the case in different suite should be different
            for group in all_groups:
                group_list = group_cases[group]
                if case in group_list:
                    select_group_list = group_list
                    break
            if not select_group_list:
                select_group_list = cases
            break
    for id1,case in enumerate(select_group_list):        
        if case!='Average' and case!='Gap':
            case_id =  case_id + 1
        else:
            continue
        totalSheet.write(row, 0, case_id,body_style_right) # write id
        col = 1
        first_data = 0
        second_data = 0
        compare_data = 0
        compare_average = 0
        num = re.compile(r'\d')
        position_all = {}  # the position value will be:  {title:[(r1,c1),(r2,c2)]}
        for id2,item in enumerate(title):
            input_value =  csv_dic[case][item]
            
            
            if item.find('##')!= -1 and num.search( input_value ):  # the second part of data titles should be ##**, ##***
                body_style.num_format_str = '0.00%'
                body_style_left.num_format_str = '0.00%'
                body_style_right.num_format_str = '0.00%'
                body_style_red.num_format_str = '0.00%'
                body_style_green.num_format_str = '0.00%'
                body_style_left_red.num_format_str = '0.00%'
                body_style_left_green.num_format_str = '0.00%'
            elif item.find('_Average')!= -1:
                
                body_style.num_format_str = '0.00%'
                body_style_left.num_format_str = '0.00%'
                body_style_right.num_format_str = '0.00%'
            else:
                body_style.num_format_str = 'general'
                body_style_left.num_format_str = 'general'
                body_style_right.num_format_str = 'general'
                body_style_red.num_format_str = 'general'
                body_style_green.num_format_str = 'general'
                body_style_left_red.num_format_str = 'general'
                body_style_left_green.num_format_str = 'general'
            try:
                if input_value.find('%')!= -1:
                    input_value = float( input_value.split('%')[0])/100.0
                else:
                    input_value = float(input_value)
            except Exception,e:
                #print e,input_value
                pass
            original_input_value = input_value
            if id2 == 0:
                totalSheet.write(row, col, input_value,body_style_right) # write case name
            elif item.find('#') != -1 and second_data == 0:  # the second file value begin
                second_data = 1
                totalSheet.write(row, col, input_value,body_style_left)
            elif item.find('##') != -1 and compare_data == 0:   # the first compare part value begin
                compare_data = 1
                aa = str( type(input_value))
                if aa.find("float")!= -1:
                    if item[2:].lower() in red_range_dic.keys():
                        red_range = float( red_range_dic[item[2:].lower()] )
                    else:
                        red_range = float( red_range_dic.get['default','0.05'])
                    print red_range
                    #--------------------------This is for the format -----------#
                    try:
                        key_position =  item[2:]
                        #print key_position
                        position1_value = position_all[key_position][0]
                        #print position1_value,position1_value[1],position1_value[0]
                        position1_value = excel_col_names[position1_value[1]]+str(position1_value[0])
                        position2_value = position_all[key_position][1]
                        position2_value = excel_col_names[position2_value[1]]+str(position2_value[0])
                        forma11 = position2_value+'*1' + '/'+position1_value+'*1' + '-1'
                        #print forma11
                        input_value = Formula(forma11)
                    except:
                        pass
                    #forma = position_all[key_position]
                    #--------------------------END-------------------------------#    
                    if item.lower().find('fmax')!= -1:
                        if original_input_value >= red_range:
                           totalSheet.write(row, col, input_value,body_style_left_green)
                           
                        elif original_input_value < 0-red_range:
                           totalSheet.write(row, col, input_value,body_style_left_red)
                        else:
                           totalSheet.write(row, col, input_value,body_style_left)
                    else:
                        if original_input_value >= red_range:
                           totalSheet.write(row, col, input_value,body_style_left_red)
                           
                        elif original_input_value < 0-red_range:
                           totalSheet.write(row, col, input_value,body_style_left_green)
                        else:
                           totalSheet.write(row, col, input_value,body_style_left)
                else:       
                    totalSheet.write(row, col, input_value,body_style_left)
                #body_style_left.font.colour_index = 'black'
            elif id2 == len(title) -1:  # the last value in the line
                totalSheet.write(row, col, input_value,body_style_right)
            elif item.find('_Average')!= -1 and compare_average == 0: # the second compare part value begin
                compare_average = 1
                totalSheet.write(row, col, input_value,body_style_left)
        
            else:
                if item.find('##')!= -1: #the first compare data
                    aa = str( type(input_value))
                    if aa.find("float")!= -1:
                        #--------------------------This is for the format -----------#
                        try:
                            key_position =  item[2:]
                            #print key_position
                            position1_value = position_all[key_position][0]
                            #print position1_value,position1_value[1],position1_value[0]
                            position1_value = excel_col_names[position1_value[1]]+str(position1_value[0])
                            position2_value = position_all[key_position][1]
                            position2_value = excel_col_names[position2_value[1]]+str(position2_value[0])
                            forma11 = position2_value+'*1' + '/'+position1_value+'*1' + '-1'
                            #print forma11
                            input_value = Formula(forma11)
                        except:
                            pass
                        #forma = position_all[key_position]
                        #--------------------------END-------------------------------#
                        if item.lower().find('fmax')!= -1:
                            if original_input_value >= red_range:
                                totalSheet.write(row, col, input_value,body_style_green)
                                print 'sss',original_input_value
                           
                            elif original_input_value < 0-red_range:
                               totalSheet.write(row, col, input_value,body_style_red)
                            else:
                               totalSheet.write(row, col, input_value,body_style)
                        else:
                            if original_input_value >= red_range:
                                totalSheet.write(row, col, input_value,body_style_red)
                           
                            elif original_input_value < 0-red_range:
                               totalSheet.write(row, col, input_value,body_style_green)
                            else:
                               totalSheet.write(row, col, input_value,body_style)
                    else:       
                        totalSheet.write(row, col, input_value,body_style)
                    #body_style.font.colour_index = 'black'
                else:
                    totalSheet.write(row, col, input_value,body_style)
                    #---------------------- The follow if used for format -------------------------#
                    if item.find('##') == -1 and item.find('_Average') == -1 :
                        if item.find('#')!= -1:
                            position_all.setdefault(item[1:],[]).append( (row+1,col) )
                        else:
                            position_all.setdefault(item,[]).append( (row+1,col) )
                            
                        pass
                    else:
                        pass
                    pass
                    #----------------------- End ------------------------------------#
            #print position_all  # the position value will be:  {title:[(r1,c1),(r2,c2)]}
            col_names.setdefault(item,[]).append( excel_col_names[col] )
            row_names.setdefault(item,[]).append(row+1)
            col = col + 1
            body_style.num_format_str = 'general'
            body_style_left.num_format_str = 'general'
            body_style_right.num_format_str = 'general'
            body_style_red.num_format_str = 'general'
            body_style_green.num_format_str = 'general'
            body_style_left_red.num_format_str = 'general'
            body_style_left_green.num_format_str = 'general'            
            '''    
            try:
                totalSheet.write(row, col, input_value,statistic_style)
            except:
                totalSheet.write(row, col, '_',statistic_style)
            col = col + 1 '''
        row = row + 1 
                               
    if 'Average' in cases:    # Write average
        col = 2
        #print col_names
        #print row_names
        totalSheet.write_merge(row,row,0,1,'Average',statistic_style)
        for id2,item in enumerate(title[1:]):
            position= []
            if item in col_names.keys():
                for id1,item2 in enumerate( col_names[item] ):
                   # print item2
                    #print id1
                    hah = item2+str( row_names[item][id1] )
                    position.append(hah )
                    #print position
                position = position[0]+":"+position[-1]
                #position = ",".join(position)
                #print position
                forma = "AVERAGE("+position+")"
                
                #print forma
            input_value =  csv_dic['Average'][item]
            if item.find('##')!= -1 and num.search( input_value ):
                    statistic_style.num_format_str = '0.00%'
            elif item.find('_Average')!= -1:
                    
                    statistic_style.num_format_str = '0.00%'
            else:
                    statistic_style.num_format_str = 'general'
            try:
                if input_value.find('%')!= -1:
                    input_value = float( input_value.split('%')[0])/100.0
                else:
                    input_value = float(input_value)
            except Exception,e:
                #print e,input_value
                pass
            try:
                #totalSheet.write(row, col, input_value,statistic_style)  #the old
                if input_value == '_' or input_value == '-':
                    totalSheet.write(row, col, input_value,statistic_style)
                else:
                    totalSheet.write(row, col,Formula(forma),statistic_style ) # this wil be the average data
                
            except Exception,e:
                #print e
                totalSheet.write(row, col, '_',statistic_style)
            col = col + 1
    statistic_style.num_format_str = 'general'
    row =  row + 1
    
    
    if 'Gap' in cases:    # Write Gap
        col = 2
        totalSheet.write_merge(row,row,0,1,'Gap',statistic_style)
        for id2,item in enumerate(title[1:]):
            position= []
            if item in col_names.keys():
                for id1,item2 in enumerate( col_names[item] ):
                   # print item2
                    #print id1
                    hah = item2+str( row_names[item][id1] )
                    position.append(hah )
                    #print position
                position = position[0]+":"+position[-1]
                #position = ",".join(position)
                #print position
                forma1 = "max("+position+")"
                forma2 = "min("+position+")"
            forma  =  forma1 + '-'+forma2
            input_value =  csv_dic['Gap'][item]
            if item.find('##')!= -1 and num.search( input_value ):
                    statistic_style.num_format_str = '0.00%'
            elif item.find('_Average')!= -1:
                    
                    statistic_style.num_format_str = '0.00%'
            else:
                    statistic_style.num_format_str = 'general'
            try:
                if input_value.find('%')!= -1:
                    input_value = float( input_value.split('%')[0])/100.0
                else:
                    input_value = float(input_value)
            except Exception,e:
                #print e,input_value
                pass
            #----------------------------this is the old not use the forma --------------------#
            #try:
            #    totalSheet.write(row, col, input_value,statistic_style)
            #except:
            #    totalSheet.write(row, col, '_',statistic_style)
            #-------------------------------End ---------------------------#
            if input_value != '_' or input_value!= '-':
                totalSheet.write(row, col,Formula(forma),statistic_style )
            else:
                totalSheet.write(row, col, input_value,statistic_style)
            col = col + 1
    statistic_style.num_format_str = 'general'
    row =  row + 1    
    return row
def write_sumary(sumary_lines,totalSheet,row):
    statistic_style = style.myCreateStyle(myBorders='2222' )
    statistic_tilte_style = style.myCreateStyle(myBorders='2222' )
    statistic_tilte_style.font.bold = 'True'
    space = re.compile(r'[^\s]')
    not_just_num = re.compile(r'[^\d]')
    for line in sumary_lines:
        line = line.split(',')
        col = 1
        for item in line:
            if space.search(item) and item.find('Sumary')==-1:
                try:
                    item  = int(item)
                except:
                    pass
                if not_just_num.search(str(item)):
                    totalSheet.write(row,col,item,statistic_tilte_style)
                else:
                    totalSheet.write(row,col,item,statistic_style)
            col = col + 1
        row =  row + 1
    
def write_statistic(statistic,summary,line):
     
    #------------------- summary structrue -----------------------#
    # {sheetname:[{tilte(slice):{  key(less15_5):value(1)},  },{title(slice):value(0.0%)}]}
    
    #--------------------------------------------------------------#
    priority=  ['xo2','ecp3','xo','xp2','ecp2','sc']
    priority_data = {}  #{key(xo2):[sheetnames,], }
    for sheet_f in summary.keys():
        f  = sheet_f.split('_')[1]  # the sheet should be "**_**_"
        f = f.lower()
        if f in priority:
            priority_data.setdefault(f,[]).append(sheet_f)
        else:
            priority_data.setdefault('sc',[]).append(sheet_f)
    for item in priority:
        sheets = priority_data.get(item,'')
        if sheets:
            pass
        else:
            continue
        sheet_percent_titles = []
        for s in sheets:
            sheet_percent_titles = sheet_percent_titles + (summary[s][1]).keys()
        sheet_percent_titles.sort()
        sheet_percent_titles = set(sheet_percent_titles)  # get all the titles
        print item
        print sheet_percent_titles
        for s in sheets:
            print summary[s][1]
        #------------------- the second part data -----------------
        sheet_row_titles = []
        for s in sheets:
            sheet_row_titles = sheet_row_titles + (summary[s][0]).keys()
        sheet_row_titles.sort()  # this is the row title
        print sheet_row_titles
        print statistic_tie_list # this is the col title
        for s in sheets:
            sheet_data = summary[s]
            print sheet_data[1]
            
            
            
    statistic_percent = style.myCreateStyle(myBorders='2222',myFormat='0.00%' )
    bold_title = style.myCreateStyle(myBorders='2222',myFont='black' )  
    bold_title.font.bold = True
    bold_title_link = style.myCreateStyle(myBorders='2222',myFont='blue' )
    bold_title_link.font.bold = True
    basename,subffix = os.path.splitext(( summary.keys())[0] ) #this is excel sheet name 
    value = ( summary.values())[0][0]  # value is a key
    average_dic = ( summary.values())[0][1]
    average_dic_keys1 = average_dic.keys()
    average_dic_keys2 = [item[1:].lower() for item in average_dic_keys1]
    col = 1+3
    if not line:
        line = 10
    for_link = basename.split('_')
    for_link = for_link[:2]
    for_link = '_'.join(for_link)
    statistic.write(line,col,for_link,bold_title_link)
    sheets = "#'"+ basename +"'!"+"A2"
    statistic.set_link(line,col,sheets,description=basename)
    id1 = 0
    for id1,key in enumerate( average_dic_keys1 ): #key_order
        statistic.write(line+id1+ 1,col-1,key,bold_title)
        data_wr = (average_dic[key]).split("%")
        data_wr = data_wr[0]
        try:
            data_wr = float(data_wr) / 100.0
        except:
            data_wr = '_'    
        statistic.write(line+id1+ 1,col,data_wr,statistic_percent)
    #-------------------     
    titles = value.keys()
    titles.sort()
    s_titles = ( value[titles[0]]).keys()
    s_titles.sort()
    statistic_tie_list
    for id1,title in enumerate(titles):
        col = 5+3
        statistic.write(line,col+id1+1,title,bold_title)  # the row title
        if id1 == 0:  # write the col title
            for id2,s_t in enumerate(statistic_tie_list):
                s_t_2 = s_t[1]
                statistic.write(line+1+id2,col,s_t_2,bold_title)  # the col title
        for id2,s_t in enumerate(statistic_tie_list):
            statistic.write(line+1+id2,col+id1+1,value[title][s_t[0]],bold_title) 
    return line+1+id2

def write_statistic_prioity(statistic,summary,line):
    #------------------- summary structrue -----------------------#
    # {sheetname:[{tilte(slice):{  key(less15_5):value(1)},  },{title(slice):value(0.0%)}]}
    
    #--------------------------------------------------------------#
    priority=  ['xo2','ecp3','xo','xp2','ecp2','sc']
    priority_data = {}  #{key(xo2):[sheetnames,], }
    ###################
    # 1  get all the priority data as a dictionary
    # 2  output the data as priority
    #    1 output the percent data
    #        1: get all the keys
    #        2: print data
    #    2 output the statistic data
    #        1: get row title
    #        2: get col title
    #        3: output datas
    for sheet_f in summary.keys():
        f  = sheet_f.split('_')[1]  # the sheet should be "**_**_"
        f = f.lower()
        if f in priority:
            priority_data.setdefault(f,[]).append(sheet_f)
        else:
            priority_data.setdefault('sc',[]).append(sheet_f)
    for item in priority:
        sheets = priority_data.get(item,'')
        if sheets:
            pass
        else:
            continue
        sheet_percent_titles = []
        for s in sheets:
            sheet_percent_titles = sheet_percent_titles + (summary[s][1]).keys()
        sheet_percent_titles = set(sheet_percent_titles)  # get all the titles
        sheet_percent_titles.sort()
        print item
        print sheet_percent_titles
        for s in sheets:
            print summary[s][1]
            
        #------------------- the second part data -----------------
        sheet_row_titles = []
        for s in sheets:
            sheet_row_titles = sheet_row_titles + (summary[s][0]).keys()
        sheet_row_titles.sort()  # this is the row title
        print statistic_tie_list # this is the col title
        for s in sheets:
            sheet_data = summary[s]
            print sheet_data[1]
            
               
        
        
        
            
            
    
    statistic_percent = style.myCreateStyle(myBorders='2222',myFormat='0.00%' )
    bold_title = style.myCreateStyle(myBorders='2222',myFont='black' )  
    bold_title.font.bold = True
    bold_title_link = style.myCreateStyle(myBorders='2222',myFont='blue' )
    bold_title_link.font.bold = True
    basename,subffix = os.path.splitext(( summary.keys())[0] ) #this is excel sheet name 
    average_dic = ( summary.values())[0][1]
    average_dic_keys1 = average_dic.keys()
    average_dic_keys2 = [item[1:].lower() for item in average_dic_keys1]
    value = ( summary.values())[0][0]
    col = 1+3
    if not line:
        line = 10
    statistic.write(line,col,basename,bold_title_link)
    sheets = "#'"+ basename +"'!"+"A2"
    statistic.set_link(line,col,sheets,description=basename)
    id1 = 0
    for id1,key in enumerate( average_dic_keys1 ): #key_order
        statistic.write(line+id1+ 1,col-1,key,bold_title)
        data_wr = (average_dic[key]).split("%")
        data_wr = data_wr[0]
        try:
            data_wr = float(data_wr) / 100.0
        except:
            data_wr = '_'    
        statistic.write(line+id1+ 1,col,data_wr,statistic_percent)
    #-------------------     
    titles = value.keys()
    titles.sort()
    s_titles = ( value[titles[0]]).keys()
    s_titles.sort()
    statistic_tie_list
    for id1,title in enumerate(titles):
        col = 5+3
        statistic.write(line,col+id1+1,title,bold_title)  # the row title
        if id1 == 0:  # write the col title
            for id2,s_t in enumerate(statistic_tie_list):
                s_t_2 = s_t[1]
                statistic.write(line+1+id2,col,s_t_2,bold_title)  # the col title
        for id2,s_t in enumerate(statistic_tie_list):
            statistic.write(line+1+id2,col+id1+1,value[title][s_t[0]],bold_title) 
    return line+1+id2
    
    
                
            
        
        
    
def write_excel(wb,csv,summary={},line=''):
    
    csv_dic,title,note,sumary_lines = read_csv_note(csv)
    
    basename = os.path.basename(csv)
    basename,subffix = os.path.splitext(basename)
    totalSheet = wb.add_sheet(basename)
    row = 0
    col = 0
    dict_return = csv_compare.process_note(note[:-1])
    row = write_note(dict_return,totalSheet)
    row = row + 1
    write_file_name(note[-1],totalSheet,row)
    write_title(title,totalSheet,row+1)
    row = write_body(csv_dic,title,totalSheet,row=row+2)
    row = write_sumary(sumary_lines,totalSheet,row+1)
    if summary:
        try:
            statistic = wb.get_sheet(0)
           
        except:
            statistic = wb.add_sheet('summary')
            
        line = write_statistic(statistic,summary,line)
    return line
    
   
def option():
    p=optparse.OptionParser()
    p.add_option("-s","--csv-file",action="store",type="string",dest="csv_file",help="The csv file you want to write")
    opt,args=p.parse_args()
    return opt   
def run_main(csv_file,summary={}):
    #opt = option()
    #csv_file = opt.csv_file 
    if not csv_file:
       print '''
           Please learn how to use it through -h
           You should run it as:
           python write_excel.py --csv-file=***
           '''
       sys.exit()
    wb = Workbook()
    write_excel(wb,csv_file,summary)
    #name,xls = os.path.splitext(csv) 
    name = 'test'+'.xls'    
    wb.save(name)
       
if __name__ == '__main__':
    #p=optparse.OptionParser()
    #p.add_option("-s","--csv-file",action="store",type="string",dest="csv_file",help="The csv file you want to write")
    #opt,args=p.parse_args()
    opt = option()
    csv_file = opt.csv_file 
    if not csv_file:
       print '''
           Please learn how to use it through -h
           You should run it as:
           python write_excel.py --csv-file=***
           '''
       sys.exit()
    wb = Workbook()
    write_excel(wb,csv_file)
    name = 'test'+'.xls'    
    wb.save(name)
