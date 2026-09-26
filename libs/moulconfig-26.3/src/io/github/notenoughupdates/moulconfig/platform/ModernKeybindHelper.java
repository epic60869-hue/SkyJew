package io.github.notenoughupdates.moulconfig.platform;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;

import net.minecraft.client.input.KeyEvent;


public class ModernKeybindHelper {
    public static StructuredText getKeyName(int keyCode) { // TODO: translations
        if (keyCode == -1) {
            return StructuredText.of("NONE");
        } else if (keyCode >= 0 && keyCode <= 9) {
            return StructuredText.of("Button " + (keyCode + 1));
        } else {
            
            StructuredText keyName = MoulConfigText.wrap(InputConstants.getKey(new KeyEvent(keyCode, 0, 0)).getDisplayName());
            
            if (keyName == null) {
                keyName = StructuredText.of("???");
            }
            return keyName;
        }
    }
}
