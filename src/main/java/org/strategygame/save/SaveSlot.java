package org.strategygame.save;

/** سه اسلات دستی و یک اسلات ذخیرهٔ خودکار جدا. */
public enum SaveSlot {
    SLOT_1("slot-1", "اسلات ۱", false),
    SLOT_2("slot-2", "اسلات ۲", false),
    SLOT_3("slot-3", "اسلات ۳", false),
    AUTOSAVE("autosave", "ذخیره خودکار", true);

    private final String fileStem;
    private final String label;
    private final boolean autosave;

    SaveSlot(String fileStem, String label, boolean autosave) {
        this.fileStem = fileStem;
        this.label    = label;
        this.autosave = autosave;
    }

    public String fileName()   { return fileStem + ".json"; }
    public String label()      { return label; }
    public boolean isAutosave(){ return autosave; }
}
