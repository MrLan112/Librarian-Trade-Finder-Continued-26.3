package de.greenman999.librariantradefinder.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import de.greenman999.librariantradefinder.screens.ControlUi;

/** Optional Mod Menu integration for the Librarian Trade Finder Continued fork. */
public class ModMenuImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<ControlUi> getModConfigScreenFactory() {
        return ControlUi::new;
    }
}
