package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ControllerConfig {

    private boolean startWhenLiveValue;
    private boolean stopWhenOfflineValue;
    private int stopAfterHsValue;
    private int repeatEveryHsValue;

    @FXML
    private CheckBox startWhenLive;

    @FXML
    private CheckBox stopWhenOffline;

    @FXML
    private TextField stopAfterHs;

    @FXML
    private TextField repeatEveryHs;


    @FXML
    void saveAllOptions() {
        startWhenLiveValue = startWhenLive.isSelected();
        stopWhenOfflineValue = stopWhenOffline.isSelected();
        
        stopAfterHsValue = Integer.parseInt(stopAfterHs.getText());
        repeatEveryHsValue = Integer.parseInt(repeatEveryHs.getText());

        System.out.println(startWhenLiveValue);
    }

}
