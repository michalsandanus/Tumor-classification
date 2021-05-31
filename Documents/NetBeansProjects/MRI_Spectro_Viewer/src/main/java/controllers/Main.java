/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;

import views.MainForm;

/**
 *
 * @author Michal Sandanus
 */
public class Main {
    public static void main(String[] args) {
        MainForm mainForm = new MainForm();
        mainForm.setLocationRelativeTo(null);
        mainForm.setVisible(true);
    }
}
