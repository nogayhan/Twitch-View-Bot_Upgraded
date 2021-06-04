package controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

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
    void initialize() {
        stopAfterHs.setDisable(!stopWhenOffline.isDisable());
        stopWhenOffline.selectedProperty().addListener((observable, oldValue, newValue) -> stopAfterHs.setDisable(newValue));
    }

    @FXML
    void saveAllOptions() {
        startWhenLiveValue = startWhenLive.isSelected();
        stopWhenOfflineValue = stopWhenOffline.isSelected();


        stopAfterHsValue = Integer.parseInt(stopAfterHs.getText());
        repeatEveryHsValue = Integer.parseInt(repeatEveryHs.getText());
    }

    public boolean isStartWhenLiveValue() {
        return startWhenLiveValue;
    }

    public boolean isStopWhenOfflineValue() {
        return stopWhenOfflineValue;
    }

    public int getStopAfterHsValue() {
        return stopAfterHsValue;
    }

    public int getRepeatEveryHsValue() {
        return repeatEveryHsValue;
    }
}
