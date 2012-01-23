package dr.evomodel.epidemiology;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class DynamicalSystem {

    private List<DynamicalVariable> variables = new ArrayList<DynamicalVariable>();
    private List<DynamicalForce> forces = new ArrayList<DynamicalForce>();
    private HashMap<String,DynamicalVariable> varMap = new HashMap<String,DynamicalVariable>();
    private HashMap<String,DynamicalForce> forceMap = new HashMap<String,DynamicalForce>();
    private double currentTime = 0.0;
    private double timeStep = 0.0;

    private double storedTime = 0.0;

    public static void main(String[] args) {

        DynamicalSystem syst = new DynamicalSystem(0.001);

        double transmissionRate = 0.027;
        double recoveryRate = 0.00054;

        syst.addVariable("susceptibles", 330);
        syst.addVariable("infecteds", 23325);
        syst.addVariable("recovereds", 4524);
        syst.addVariable("total", 330 + 23325 + 4524);
        syst.addForce("contact", transmissionRate, new String[]{"infecteds","susceptibles"}, new String[]{"total"}, "susceptibles", "infecteds");
        syst.addForce("recovery", recoveryRate, new String[]{"infecteds"}, new String[]{}, "infecteds", "recovereds");

//        while (syst.getTime() < 400) {
//            syst.step();
//        }
//        syst.print(0,400,1);

        double val = syst.getValue("susceptibles", 500);
        System.out.println(val);
        syst.print(0,500,10);

    }

    public DynamicalSystem(double dt) {
        currentTime = 0.0;
        timeStep = dt;
    }

    public double getTime() {
        return currentTime;
    }

    public int size() {
        return variables.size();
    }

    public DynamicalVariable getVar(String n) {
        return varMap.get(n);
    }

   public DynamicalForce getForce(String n) {
        return forceMap.get(n);
    }

    public void resetVar(String n, double v0) {
        DynamicalVariable var = getVar(n);
        var.reset(v0);
    }

    public void resetForce(String n, double c) {
        DynamicalForce frc = getForce(n);
        frc.reset(c);
    }

    public void resetTime() {
        currentTime = 0.0;
    }

    // copy values to stored state
    public void store() {
        storedTime = currentTime;
        for (DynamicalVariable var : variables) {
            var.store();
        }
        for (DynamicalForce frc : forces) {
            frc.store();
        }
    }

    // copy values from stored state
    public void restore() {
        currentTime = storedTime;
        for (DynamicalVariable var : variables) {
            var.restore();
        }
        for (DynamicalForce frc : forces) {
            frc.restore();
        }
    }

    // get value of indexed variable at time t
    // dynamically extend trace
    public double getValue(int index, double t) {
        while (currentTime < t) {
            step();
        }
        DynamicalVariable var = variables.get(index);
        return var.getValue(t);
    }

    // get value of named variable at time t
    // dynamically extend trace
    public double getValue(String n, double t) {
        while (currentTime < t) {
            step();
        }
        DynamicalVariable var = getVar(n);
        return var.getValue(t);
    }

    // get integral of indexed variable between times start and finish
    // dynamically extend trace
    public double getIntegral(int index, double start, double finish) {
        while (currentTime < finish) {
            step();
        }
        DynamicalVariable var = variables.get(index);
        return var.getIntegral(start, finish);
    }

    // get integral of named variable between times start and finish
    // dynamically extend trace
    public double getIntegral(String n, double start, double finish) {
        while (currentTime < finish) {
            step();
        }
        DynamicalVariable var = getVar(n);
        return var.getIntegral(start, finish);
    }

    public void addVariable(String n, double v0) {
        DynamicalVariable var = new DynamicalVariable(n, v0);
        varMap.put(n, var);
        variables.add(var);
    }

    public void addForce(String n, double coeff, String[] mult, String[] div, String increasing, String decreasing) {
        DynamicalForce frc = new DynamicalForce(n, coeff, getVar(increasing), getVar(decreasing));
        for (String s : mult) {
            frc.addMultiplier(getVar(s));
        }
        for (String s : div) {
            frc.addDivisor(getVar(s));
        }
        forceMap.put(n, frc);
        forces.add(frc);
    }

    public void step() {
        for (DynamicalForce frc : forces) {
            frc.modCurrentValue(currentTime, timeStep);
        }
        for (DynamicalVariable var : variables) {
            var.modCurrentTime(timeStep);
            var.pushCurrentState();
        }
        currentTime += timeStep;
    }

    public void print(double start, double finish, double step) {

        System.out.print("time");
        for (DynamicalVariable var : variables) {
            System.out.print("\t" + var.getName());
        }
        System.out.println();

        for (double t=start; t<=finish; t += step) {
            System.out.printf("%.3f", t);
            for (DynamicalVariable var : variables) {
                System.out.printf("\t%.3f", var.getValue(t));
            }
            System.out.println();
        }

    }

    public String printValues(String n, double start, double finish, double step) {

        String out = "";
        DynamicalVariable var = getVar(n);
        for (double t=start; t<=finish; t += step) {
            double v = var.getValue(t);
            out += "\t";
            out += Double.toString(v);
        }
        return out;

    }

}