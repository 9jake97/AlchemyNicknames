package com.github.plunk.alchemypersona.joinmessages.gui;

import java.util.List;
import org.bukkit.Material;

public class DisplayItem {
    private Material material;
    private short data;
    private String name;
    private List<String> lore;

    public DisplayItem(Material material, short data, String name, List<String> lore) {
        this.setMaterial(material);
        this.setData(data);
        this.setName(name);
        this.setLore(lore);
    }

    public Material getMaterial() {
        return this.material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public short getData() {
        return this.data;
    }

    public void setData(short data) {
        this.data = data;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLore() {
        return this.lore;
    }

    public void setLore(List<String> lore) {
        this.lore = lore;
    }
}


