package com.jmfavreau.pictoparle.core;

import android.util.Log;

import com.jmfavreau.pictoparle.PictoParleActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class Device {


    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceKey() {
        return deviceKey;
    }

    public Float getScreenWidth() {
        return screenWidth;
    }

    public Float getScreenHeight() {
        return screenHeight;
    }

    private String deviceName;
    private String deviceKey;
    private Float screenWidth;
    private Float screenHeight;

    public Device(XmlPullParser parser) throws XmlPullParserException, IOException {
        deviceKey = "";
        deviceName = "";
        screenHeight = -1f;
        screenWidth = -1f;
        loadFromParser(parser);
    }
    public Device() {
        deviceKey = "custom";
        deviceName = "Personnaliser la résolution...";
        screenWidth = -1f;
        screenHeight = -1f;
    }

    private void loadFromParser(XmlPullParser parser) throws XmlPullParserException, IOException {
        do {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "device":
                        this.deviceName = parser.getAttributeValue(null, "name");
                        this.deviceKey = parser.getAttributeValue(null, "id");
                        break;

                    case "screen":
                        this.screenWidth = Float.parseFloat(parser.getAttributeValue(null, "width"));
                        this.screenHeight = Float.parseFloat(parser.getAttributeValue(null, "height"));
                        break;
                }
            }
        } while (parser.next() != XmlPullParser.END_DOCUMENT);
    }

    public boolean isValid() {
        return deviceName != "" && deviceKey != "" && screenWidth > 0 && screenHeight > 0;
    }
}
