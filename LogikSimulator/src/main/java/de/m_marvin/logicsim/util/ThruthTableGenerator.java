package de.m_marvin.logicsim.util;

import de.m_marvin.logicsim.logic.Circuit;
import de.m_marvin.logicsim.logic.Component;
import de.m_marvin.logicsim.logic.parts.ButtonComponent;
import de.m_marvin.logicsim.logic.parts.LampComponent;

import java.util.ArrayList;
import java.util.List;

public class ThruthTableGenerator {

    public static List<ButtonComponent> getInputs(Circuit ci) {
        List<ButtonComponent> inputs = new ArrayList<>();
        for (Component c : ci.getComponents()) {if (c instanceof ButtonComponent) {inputs.add((ButtonComponent) c);}}
        return inputs;
    }

    public static List<LampComponent> getOutputs(Circuit ci) {
        List<LampComponent> inputs = new ArrayList<>();
        for (Component c : ci.getComponents()) {if (c instanceof LampComponent) {inputs.add((LampComponent) c);}}
        return inputs;
    }

    public static String add0(String sIn, int amount) {
        StringBuilder sInBuilder = new StringBuilder(sIn);
        sInBuilder.append("0".repeat(Math.max(0, amount)));
        sIn = sInBuilder.toString();
        return sIn;
    }

    public static String reverse(String s){
        return new StringBuilder(s).reverse().toString();
    }

    public static String add0front(String s, int amount){
        return reverse(add0(reverse(s), amount));
    }

    public static String add0frontToLength(String s, int target){
        return add0front(s, target-s.length());
    }

    public static String getThruthTableFromCircuit(Circuit ci) {
        List<ButtonComponent> inputs = getInputs(ci);

        StringBuilder table = new StringBuilder();

        int val = 0;
        while(val < Math.pow(2, inputs.size())) {
            resetInputs(ci);
            resetOutputs(ci);
            String val_b = add0(reverse(Integer.toBinaryString(val)),inputs.size());
            int it = 0;
            for(ButtonComponent i : inputs) {
                if(val_b.charAt(it) == '1') {
                    i.toggle = true;
                }
                it++;
            }

            ci.updateCircuit();

            for (int i = 0; i < ci.getComponents().size(); i++) {
                ci.updateCircuit();
            }

            StringBuilder output = new StringBuilder();

            for (Component o : ci.getComponents()) {
                if (o instanceof LampComponent) {
                    boolean s = ((LampComponent) o).state;
                    int s_int = !s ? 0 : 1;
                    output.append(s_int);
                }
            }

            String outs = add0frontToLength(Integer.toBinaryString(val), inputs.size());
            table.append(outs).append(" || ").append(output).append("\n");

            val++;
        }

        resetInputs(ci);
        resetOutputs(ci);

        return table.toString();
    }

    public static void resetInputs(Circuit ci) {
        List<ButtonComponent> inputs = getInputs(ci);
        for (ButtonComponent c : inputs) {c.toggle = false;}
    }

    public static void resetOutputs(Circuit ci) {
        List<LampComponent> inputs = getOutputs(ci);
        for (LampComponent c : inputs) {c.state = false;}
    }

}
