package gui.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import db.Data;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.Parent;

public class displayConfigurationController implements Initializable{

    @FXML
    private Button button_saveView;

    @FXML
    private TextField input_headerText;

    @FXML
    private TextField input_middleText;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		if(Data.getCurrentConfiguration().getHeader_text() != null) {
			input_headerText.setText(Data.getCurrentConfiguration().getHeader_text());
		}
		if(Data.getCurrentConfiguration().getMiddle_text() != null) {
			input_middleText.setText(Data.getCurrentConfiguration().getMiddle_text());
		}
		
		
		button_saveView.setOnAction((EventHandler<ActionEvent>) new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				
				
				Data.getCurrentConfiguration().setHeader_text(input_headerText.getText());
				Data.getCurrentConfiguration().setMiddle_text(input_middleText.getText());
				try {
					Data.saveData();
				} catch (ClassNotFoundException | IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				System.out.println("Ustawiono tekst na " + Data.getCurrentConfiguration().getHeader_text());
				
				try {
					Parent root = FXMLLoader.load(getClass().getResource("/fxml/configScreen.fxml"));
					button_saveView.getScene().setRoot(root);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			}
			
			
		});
		
		
	}
    
    
    
    
    
    

}