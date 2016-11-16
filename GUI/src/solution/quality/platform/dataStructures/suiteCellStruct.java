/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.dataStructures;

import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.util.Callback;
import solution.quality.platform.SystemsQualityPlatform;
import solution.quality.platform.utils;
import solution.quality.platform.views.ListViewCellController;

/**
 *
 * @author nmallam1
 */
public class suiteCellStruct {
   
    
    public suiteCellStruct(ListView viewSuite, ListView viewTC) {     
        viewSuite.setCellFactory(new CallbackImpl());
        viewTC.setCellFactory(new CallbackImpl()); 
        
        viewSuite.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        viewTC.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
    }

    private void setLV(List<String> list, ListView view) {
        view.setItems(null);
        ObservableList<String> i = FXCollections.observableList(list);
        view.setItems(i);
    } 
    
    
    private String getName(String s) {
        return utils.getNameFromKey(s);
    }
    
    private int getStatus(String s) {
        return utils.getStatusFromKey(s);
    }
    
    
    
    public void setSuiteData(ListView viewSuite, List<String> statusList) {
//        List<String> list = new ArrayList<>();
//        
//        List<casesStruct> suiteDataList = data.getSuites();
//        if(suiteDataList != null) {
//            for(casesStruct c: suiteDataList) {
//                list.add(c.getSuiteName());
//            }
//
//            setLV(list, viewSuite);
//        }

        if(statusList != null) 
            setLV(statusList, viewSuite);
    }
    

    public void setTCData(ListView viewTC, List<String> tcStatusList) {
//        List<String> list = new ArrayList<>();
//        
//        List<casesStruct> suiteDataList = data.getSuites();
//        for(casesStruct c: suiteDataList) {
//            if(c.getSuiteName().equals(suiteName))
//                list = c.getTestCases();
//        }
//        
//        if(list != null)
//            setLV(list, viewTC);

        if(tcStatusList != null) {
            setLV(tcStatusList, viewTC);
        }

    }
    
    private void setLVP(List<String> list, ListView view) {
        ObservableList<String> i = FXCollections.observableList(list);
        view.setItems(i);
    } 
    public void setSuiteDataP(ListView viewSuite, List<String> statusList) {
        if(statusList != null) 
            setLVP(statusList, viewSuite);
    }
    public void setTCDataP(ListView viewTC, List<String> tcStatusList) {
        if(tcStatusList != null) {
            setLVP(tcStatusList, viewTC);
        }

    }

    public class listViewCell extends ListCell<String> {
    
        String name;
        String stat;

        FXMLLoader container = new FXMLLoader(SystemsQualityPlatform.class.getResource("views/listViewCell.fxml"));
        
        public listViewCell() {
            super();
            try {
                container.load();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            
            setText(null);
            if(item == null) {
                setGraphic(null);
            }
            if(!empty) {
                String sName = getName(item);
                int sStat = getStatus(item);
                this.name = sName;
                //System.out.println(sStat + ":" + sName + "---" + item); 
                ListViewCellController lc = container.<ListViewCellController>getController();
                //lc.setName(item);
                lc.setName(sName);
                //lc.setStatus(1);
                lc.setStatus(sStat);
    
                
                //System.out.println("Set status " + sStat);
                setGraphic(lc.getAP());
            }
        }
        

        @Override
        public String toString() {
            return this.name;
        }



 
    }
    

    private class CallbackImpl implements Callback<ListView<String>, ListCell<String>> {

        @Override
        public ListCell<String> call(ListView<String> param) {
            return new listViewCell();
        }
    }
    
}
