package com.inventory;

/** Small demo showing the CRUD API in action. */
public class Main {
    public static void main(String[] args) {
        InventoryService inventory = new InventoryService();

        inventory.addItem(new Item("A1", "Laptop", 10, 50000));
        inventory.addItem(new Item("A2", "Mouse", 3, 500));
        System.out.println("After create : " + inventory.getAllItems());

        inventory.updateQuantity("A2", 25);
        System.out.println("After update : " + inventory.getItem("A2").orElseThrow());

        inventory.deleteItem("A1");
        System.out.println("After delete : " + inventory.getAllItems());
        System.out.printf("Total value  : %.2f%n", inventory.getTotalInventoryValue());
    }
}
