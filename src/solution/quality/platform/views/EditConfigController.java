/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package solution.quality.platform.views;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import solution.quality.platform.utils;

/**
 * FXML Controller class
 *
 * @author nmallam1
 */
public class EditConfigController implements Initializable {

    /**
     * Initializes the controller class.
     */
    
    @FXML TextField defaultAddressTF;
    @FXML Button setButton;
    
    @FXML TextField commentTF;
    @FXML Button commentAddButton;
    @FXML Button commentAddInLineButton;
    
    @FXML TextField addrhTF;
    @FXML TextField addrlTF;
    @FXML TextField burstLenTF;
    @FXML TextField boardTF;
    @FXML TextField rbopTF;
    @FXML TextField amodTF;
    @FXML TextField wrnTF;
    @FXML TextField dataTF;
    
    
    @FXML Button addButton;
    
    @FXML ListView previewLV;
    @FXML Label previewTitle;
    
    @FXML Button modifyButton;
    @FXML Button deleteButton;
    
    @FXML TextField nameTF;
    @FXML Button createFileButton;
    @FXML Button saveAsButton;
    
    List<ConfData> list;
    
    boolean firstClick = true;
    boolean hasAddress = false;
    boolean inLineCommentLock = false;
    
    String SESSION_ADDRESS;
    
    String PATH;
    String EXTN = ".txt";
    String CONFPATH;
    
    String INPUTDIRPATH;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setUnEditable();
        setVisiblePreview(false);
        setButtonsVisibility(false);
        setCharLimits();
        list = new ArrayList<>();
        CONFPATH = null;
    }
    
    public void init(String PATH) {
        this.SESSION_ADDRESS = null;
        this.PATH = PATH;
        this.INPUTDIRPATH = PATH + utils.SEPARATOR + "sim" + utils.SEPARATOR + "input_files";
    }
    
    private void setUnEditable() {
        boardTF.setEditable(false);
        boardTF.setMouseTransparent(true);
    }
    private void setVisiblePreview(boolean bool) {
        previewLV.setVisible(bool);
        previewTitle.setVisible(bool);
    }
    
    private void setButtonsVisibility(boolean bool) {
        modifyButton.setVisible(bool);
        deleteButton.setVisible(bool);
    }
    
    private void setCharLimits() {
        addrhTF.addEventFilter(KeyEvent.KEY_TYPED, maxLength(2));
        addrlTF.addEventFilter(KeyEvent.KEY_TYPED, maxLength(2));
        burstLenTF.addEventFilter(KeyEvent.KEY_TYPED, maxLength(2));
        defaultAddressTF.addEventFilter(KeyEvent.KEY_TYPED, maxLength(3));
        rbopTF.addEventFilter(KeyEvent.KEY_TYPED, maxLength(1));
        amodTF.addEventFilter(KeyEvent.KEY_TYPED, maxLength(1));
        wrnTF.addEventFilter(KeyEvent.KEY_TYPED, maxLength(1));
    }
    
    private void setSelectedData(ConfData confData) {
        boolean flagComment = confData.getHasComment();
        boolean flagCommentCombo = confData.getInLineCommentCombo();
        
        if(flagCommentCombo) {
            commentTF.setText(confData.getComment());
            addrhTF.setText(confData.getAddrh());
            addrlTF.setText(confData.getAddrl());
            burstLenTF.setText(confData.getBurstLen());
            boardTF.setText(confData.getBoardAddress());
            rbopTF.setText(confData.getRbop());
            amodTF.setText(confData.getAmod());
            wrnTF.setText(confData.getWrn());
            dataTF.setText(confData.getPadData());
        }
        else if(flagComment) {
            commentTF.setText(confData.getComment());
        }
        else {    
            addrhTF.setText(confData.getAddrh());
            addrlTF.setText(confData.getAddrl());
            burstLenTF.setText(confData.getBurstLen());
            boardTF.setText(confData.getBoardAddress());
            rbopTF.setText(confData.getRbop());
            amodTF.setText(confData.getAmod());
            wrnTF.setText(confData.getWrn());
            dataTF.setText(confData.getPadData());
        }
    }
    
    private void changeAddress() {
        for(ConfData c: list) {
            boolean flag = c.getHasComment();
            if(!flag) {
                c.setBoardAddress(this.SESSION_ADDRESS);
                String k = c.getKey();
                System.out.println("modified key = " + k);
            }
        }
        
        previewLV.setItems(null);
        setListView();
    }
    
    @FXML
    public void setButtonAction(ActionEvent e) {
        boolean flag = defaultAddressTF.getText() != null || !(defaultAddressTF.getText().trim().isEmpty());
        if(flag) {
            this.SESSION_ADDRESS = defaultAddressTF.getText();
            boardTF.setText(this.SESSION_ADDRESS);   
            
            //modify list if address is changed (2nd time onwards)
            if(firstClick) {
                firstClick = false;
            }
            else {
                changeAddress();
            }
        }
        
        
    }
        
    
    @FXML
    public void listCellOnClick (MouseEvent e) {
        if(!previewLV.getSelectionModel().isEmpty()) {
            setButtonsVisibility(true);
            ConfData data = (ConfData) previewLV.getSelectionModel().getSelectedItem();
            setSelectedData(data);
        }
        
    }
    
    @FXML
    public void modifyData (ActionEvent e) {
        if(!previewLV.getSelectionModel().isEmpty()) {
            ConfData data = (ConfData) previewLV.getSelectionModel().getSelectedItem();
            String id = data.getID();
            for(ConfData c: list) {
                if(c.getID().equals(id)) {
                    boolean flagComment = c.getHasComment();
                    boolean flagCommentCombo = c.getInLineCommentCombo();
                    
                    if(flagCommentCombo) {
                        String comment = commentTF.getText();
                        String addrh = addrhTF.getText();
                        String addrl = addrlTF.getText();
                        String burstLen = burstLenTF.getText();
                        String boardAddress = boardTF.getText();
                        String rbop = rbopTF.getText();
                        String amod = amodTF.getText();
                        String wrn = wrnTF.getText();
                        String dataPad = dataTF.getText();
                        c.setComment(comment);
                        c.setAddrh(addrh);
                        c.setAddrl(addrl);
                        c.setBurstLen(burstLen);
                        c.setBoardAddress(boardAddress);
                        c.setRbop(rbop);
                        c.setAmod(amod);
                        c.setWrn(wrn);
                        c.setPadData(dataPad);
                    }
                    else if(flagComment) {
                        String comment = commentTF.getText();
                        c.setComment(comment);
                    }
                    else {
                        String addrh = addrhTF.getText();
                        String addrl = addrlTF.getText();
                        String burstLen = burstLenTF.getText();
                        String boardAddress = boardTF.getText();
                        String rbop = rbopTF.getText();
                        String amod = amodTF.getText();
                        String wrn = wrnTF.getText();
                        String dataPad = dataTF.getText();
                        c.setAddrh(addrh);
                        c.setAddrl(addrl);
                        c.setBurstLen(burstLen);
                        c.setBoardAddress(boardAddress);
                        c.setRbop(rbop);
                        c.setAmod(amod);
                        c.setWrn(wrn);
                        c.setPadData(dataPad);
                        String k = c.getKey();
                        System.out.println("New Key: " + k);
                    }
                }
            }
            previewLV.setItems(null);
            setListView();
            setButtonsVisibility(false);
        }
    }
    
    @FXML
    public void deleteData (ActionEvent e) {
        if(!previewLV.getSelectionModel().isEmpty()) {
            
            if(this.inLineCommentLock)
                inLineCommentLock = false;
            
            
            ConfData data = (ConfData) previewLV.getSelectionModel().getSelectedItem();
            String id = data.getID();

            for(int i = 0; i < list.size(); i++) {
                ConfData c = list.get(i);
                if(c.getID().equals(id)) {
                    previewLV.getItems().remove(c);
                    list.remove(c);
                }
            }
            
            previewLV.setItems(null);
            setListView();
            
        }
    }
    
    public EventHandler<KeyEvent> maxLength(final Integer i) {
        return (KeyEvent arg0) -> {
            TextField tx = (TextField) arg0.getSource();
            if (tx.getText().length() >= i) {
                arg0.consume();
            }
        };
    }
    
    /*
        Check if any textfield is empty
            return True if so, False otherwise
    */
    private boolean isTFEmpty() {
        if(addrhTF.getText() == null || addrhTF.getText().trim().isEmpty()) 
            return false;
        else if(addrlTF.getText() == null || addrlTF.getText().trim().isEmpty())
            return false;
        else if(burstLenTF.getText() == null || burstLenTF.getText().trim().isEmpty())
            return false;
        else if(boardTF.getText() == null || boardTF.getText().trim().isEmpty())
            return false;
        else if(rbopTF.getText() == null || rbopTF.getText().trim().isEmpty())
            return false;
        else if(amodTF.getText() == null || amodTF.getText().trim().isEmpty())
            return false;
        else if(wrnTF.getText() == null || wrnTF.getText().trim().isEmpty())
            return false;
        else return !(dataTF.getText() == null || dataTF.getText().trim().isEmpty());
        
    }
    
    private void addToList(ConfData c) {
        boolean comment = c.getHasComment();
        if(this.inLineCommentLock) {
            if(!comment) {
                this.inLineCommentLock = false;
                list.add(c);
                setListViewInLine();
            }
        }
        else {
            list.add(c);
            setListView();
        }
    }
    
    /*
        Set ListView data:
        1) add to List
        2) Create observablelits from List
        3) set listview 
    */
    private void setListView() {
        ObservableList<ConfData> i = FXCollections.observableList(list);
        previewLV.setItems(i); 
    }
    
    private void setListViewInLine() {
        List<ConfData> t = new ArrayList<>();
        int size = list.size();
        int newSize = size - 2;
        
        ConfData inLineComment = new ConfData(list.get(size - 2), list.get(size-1));
        for(int i = 0; i < newSize; i++) {
            t.add(list.get(i));
        }
        t.add(inLineComment);
        list = new ArrayList<>(t);
        ObservableList<ConfData> i = FXCollections.observableList(t);
        previewLV.setItems(i); 
    }
    
    @FXML
    public void addData(ActionEvent e) {
        if(isTFEmpty()) {
            
            String addrh = addrhTF.getText();
            String addrl = addrlTF.getText();
            String burstLen = burstLenTF.getText();
            String boardAddress = boardTF.getText();
            String rbop = rbopTF.getText();
            String amod = amodTF.getText();
            String wrn = wrnTF.getText();
            String data = dataTF.getText();
            
            ConfData confData = new ConfData(addrh, addrl, burstLen, boardAddress, rbop, amod, wrn, data);
            String key = confData.getKey();
            setVisiblePreview(true);
            //list.add(confData);
            addToList(confData);
            //setListView();
            this.hasAddress = true;
            System.out.println("Key : " + key + "\nID: " + confData.getID());

        }
    }
    
    @FXML
    public void addComment(ActionEvent e) {
        boolean flag = commentTF.getText() != null || !(commentTF.getText().trim().isEmpty());
        boolean inLineComment = false;
        if(flag) {
            ConfData comment = new ConfData(commentTF.getText(), inLineComment);
            setVisiblePreview(true);
            addToList(comment);
            //list.add(comment);
            setListView();
        }
    }
    
    @FXML
    public void addInLineComment(ActionEvent e) {
        boolean flag = commentTF.getText() != null || !(commentTF.getText().trim().isEmpty());
        boolean inLineComment = true;
        if(flag) {
            ConfData comment = new ConfData(commentTF.getText(), inLineComment);
            addToList(comment);
            this.inLineCommentLock = true;
            this.addrhTF.requestFocus();
            //list.add(comment);
            setVisiblePreview(true);
            setListView();
        }
        
    }
    
    private void writeHeader(PrintWriter pw) {
        
        String address = this.SESSION_ADDRESS;
        
        ConfData h1 = new ConfData("00", "00", "00", address, "0", "0", "1", "XX AA");
        ConfData h2 = new ConfData("00", "00", "00", address, "0", "0", "0", "55");
        ConfData h3 = new ConfData("00", "00", "00", address, "0", "0", "1", "XX 55");
        ConfData h4 = new ConfData("00", "07", "00", address, "0", "0", "0", "01");
        ConfData h5 = new ConfData("00", "07", "00", address, "0", "0", "0", "01");
        ConfData h6 = new ConfData("00", "07", "00", address, "0", "0", "0", "00");
        
        h1.getKey();
        h2.getKey();
        h3.getKey();
        h4.getKey();
        h5.getKey();
        h6.getKey();
        
        pw.println("//generated default header");
        pw.println(h1);
        pw.println(h2);
        pw.println(h3);
        pw.println(h4);
        pw.println(h5);
        pw.println(h6);
        pw.println();
    }
    
    
    private void writeToFile() {
        
        try (PrintWriter confFile = new PrintWriter(this.CONFPATH, "UTF-8")) {
            writeHeader(confFile);

            for(int i = 0; i < list.size(); i++) {
                ConfData c = list.get(i);
                if(c.getHasComment()) {
                    if(c.getInLineComment()) {
                        confFile.print(c);
                    }
                    else {
                        confFile.println(c);
                    }
                }
                else 
                    confFile.println(c);
            }
             
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    public void createFile(ActionEvent e) {
        String name = nameTF.getText();
        int size = list.size();
        
        if(name != null && !(name.trim().isEmpty()) && size > 0 && this.hasAddress) {
            this.CONFPATH = this.INPUTDIRPATH + utils.SEPARATOR +  name + EXTN;
            writeToFile();
            ((Node)(e.getSource())).getScene().getWindow().hide();
        }
    }
    
    @FXML
    public void saveAsAction(ActionEvent e) {
        String name = nameTF.getText();
        int size = list.size();
        
        if(name != null && !(name.trim().isEmpty()) && size > 0) {
            Node node = (Node) e.getSource();
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Project Destination");
            File file = chooser.showDialog(node.getScene().getWindow());
            if(file != null) {
                this.CONFPATH = this.PATH + utils.SEPARATOR + name + EXTN;
                writeToFile();
                ((Node)(e.getSource())).getScene().getWindow().hide();
            }
        }
    }
    
    
    public String getConfPath() {
        return this.CONFPATH;
    }
    
    /*
        tableView data structure
    */
    public static class ConfData {
        
        private String id;
        
        private String addrh;
        private String addrl;
        private String burstLen;
        private String boardAddress;
        private String rbop;
        private String amod;
        private String wrn;
        private String padData;
        private String comment;
        
        private boolean hasComment = false;
        private boolean inLineComment;
        private String key;
        
        private boolean inLineCommentCombo = false;
        
        
        public ConfData(String addrh, String addrl, String burstLen, String boardAddress, String rbop, String amod, String wrn, String padData) {
            this.id = genID();
            this.addrh = addrh;
            this.addrl = addrl;
            this.burstLen = burstLen;
            this.boardAddress = boardAddress;
            this.rbop = rbop;
            this.amod = amod;
            this.wrn = wrn;
            this.padData = padData;
        }
        
        public ConfData(String comment, boolean inLineComment) {
            this.id = genID();
            this.hasComment = true;
            this.comment = comment;
            this.inLineComment = inLineComment;
        }
        
        public ConfData(ConfData c, ConfData d) {
            this.id = genID();
            setCommentComboData(c,d);
            this.inLineCommentCombo = true;
        }
        
        
        private String genID() {
            return UUID.randomUUID().toString();
        }
        
        public String getID() {
            return this.id;
        }
        
        public String getAddrh() {
            return addrh;
        }
        
        public void setAddrh(String data) {
            this.addrh = data;
        }
        
        public String getAddrl() {
            return addrl;
        }
        
        public void setAddrl(String data) {
            this.addrl = data;
        }
        
        public String getBurstLen() {
            return burstLen;
        }
        
        public void setBurstLen(String data) {
            this.burstLen = data;
        }
        
        public String getBoardAddress() {
            return boardAddress;
        }
        
        public void setBoardAddress(String data) {
            this.boardAddress = data;
        }
        
        public String getRbop() {
            return rbop;
        }
        
        public void setRbop(String data) {
            this.rbop  = data;
        }
        
        public String getAmod() {
            return amod;
        }
        
        public void setAmod(String data) {
            this.amod = data;
        }
        
        public String getWrn() {
            return wrn;
        }
        
        public void setWrn(String data) {
            this.wrn = data;
        }
        
        public String getPadData() {
            return padData;
        }
        
        public void setPadData(String data) {
            this.padData  = data;
        }
        
        private void setKey() {
            String space = " ";
        
            byte byte2 = Byte.parseByte(this.burstLen + this.boardAddress + this.rbop + this.amod + this.wrn, 2);
            String byte_2 = String.format("%02d", byte2);

            String dataFinal = this.addrh + space + this.addrl + space + byte_2 + space + this.padData;
            
            this.key = dataFinal;
            
        }
        
        public String getKey() {
            setKey();
            return this.key;
        }
        
        public String getComment() {
            return this.comment;
        }
        
        public void setComment(String data) {
            this.comment = data;
        }
        
        public boolean getHasComment() {
            return this.hasComment;
        }
        
        public boolean getInLineComment() {
            return this.inLineComment;
        }
        
        public void setInLineComment(boolean data) {
            this.inLineComment = data;
        }
        
        private void setCommentComboData(ConfData c, ConfData d) {
            setComment(c.getComment());
            
            setAddrh(d.getAddrh());
            setAddrl(d.getAddrl());
            setBurstLen(d.getBurstLen());
            setBoardAddress(d.getBoardAddress());
            setRbop(d.getRbop());
            setAmod(d.getAmod());
            setWrn(d.getWrn());
            setPadData(d.getPadData());
            
        }
        
        public String commentComboKey() {
            this.key = getKey();
            return "#(" + this.comment + ")" + this.key;
        }
        
        public boolean getInLineCommentCombo() {
            return this.inLineCommentCombo;
        }
        
        @Override
        public String toString() {
            if(this.inLineCommentCombo) 
                return commentComboKey();
            if(this.hasComment)
                if(this.inLineComment)
                    return "#(" + this.comment + ")";
                else
                    return "//" + this.comment;
            else 
                return this.key;
        }
        
    }
    
}
