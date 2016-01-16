
import libsvm.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import data.ifMatrixData;
import data.ifVectorData;
import data.clVectorData;
import data.clMatrixData;
import bb.cast.ifCasteljau;
import bb.cast.clUniCasteljau;
import bb.cast.clCasteljauException;
import bb.bp.ifBernsteinPack;
import bb.bp.clUniBernsteinPolyPack;
import bb.bp.clPolynomialException;
import graph.*;
import util.clMathEx;
import util.clTracer;

class PRIAMAlgoHandler implements iAlgoHandler {
    private boolean m_stop = false;
    final private iDataHandler dataprovider;
    final private iResultHandler resulthandler;
    final JTabbedPane resulttab;
    private JTextField epsilon;
    private JTextField beta;
    private JCheckBox bruteforce;

    // transient variables:
    private svm_node[][] xt;
    private ifVectorData ymt; // real testing output
    private ifMatrixData xml;
    private ifVectorData yml;
    private clVectorData approx;
    private double[] w;
    private JProgressBar progressBar;
    private double model_bias;
    private ContoursSheet contour_sheet_element = null;
    private IndividualsSheet individuals_sheet_element = null;

    public PRIAMAlgoHandler(iDataHandler dataprovider, iResultHandler resulthandler, JTabbedPane resulttab) {
        this.dataprovider = dataprovider;
        this.resulthandler = resulthandler;
        this.resulttab = resulttab;
    }

    public void addParameters(JPanel palgo) {
        JPanel paramp = new JPanel(new GridLayout(2,2,0,0));
        paramp.add(new JLabel(StringResources.get(StringResources.eps_parameter)+":"));
        paramp.add(epsilon = new JTextField("0.021", 3));
        paramp.add(new JLabel(StringResources.get(StringResources.beta_parameter)+":"));
        paramp.add(beta = new JTextField("7.6", 3));
        palgo.setLayout(new BoxLayout(palgo, BoxLayout.Y_AXIS));
        palgo.add(paramp);
        palgo.add(bruteforce = new JCheckBox(StringResources.get(StringResources.bruteforce), false));
        palgo.add(progressBar = new JProgressBar(0, 100));
        progressBar.setStringPainted(true);
        progressBar.setString("");
    }

    public void cancelBuild() {
        m_stop = true;
    }

    public synchronized void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        m_stop = false;
        while (!m_stop) {

            if (bruteforce.isSelected()) {
                BruteForce();
                progressBar.setString("");
                progressBar.setValue(100);
            } else
                runPRIAMonce();

            m_stop = true;
        }
        notify(); // notify branched thread that algo has finished
    }

    private void BruteForce() {
        svm_problem prob = createproblem();
        svm_parameter param = createparameters();
        contourinfo conti = new contourinfo();
        final int size = 15;
        conti.e_min = 0.01;
        conti.e_max = 0.3;
        final double de = (conti.e_max - conti.e_min) / (size - 1);
        conti.b_min = 5;
        conti.b_max = 50;
        final double db = (conti.b_max - conti.b_min) / (size - 1);
        conti.mse_info = new double [size*size];
        conti.bec_info = new double [size*size];
        conti.n_b = conti.n_e = size;
        double e = conti.e_min;
        double progress_step = 100.0/(size*size);
        double progress_value = 0.0;
        for (int i = 0; i < size; i++) {
            double b = conti.b_min;
            for (int j = 0; j < size; j++) {
                if (m_stop)
                    return;
                progress_value += progress_step;
                progressBar.setString(String.valueOf((int)progress_value)+"%");
                progressBar.setValue((int)progress_value);

                param.C = b;
                param.p = e;
                ifVectorData predicted = solveproblem(prob, param);

                // calculate mse:
                final int testinglength = xt.length;
                double MSE = 0.0;
                for (int k = 0; k < testinglength; k++) {
                    MSE += (predicted.getValue(k) - ymt.getValue(k))*(predicted.getValue(k) - ymt.getValue(k));
                }
                MSE /= testinglength;
                conti.mse_info[size*j+i] = MSE;

                // calculate BEC:
                int nl = yml.getArraySize();
                double BEC = 0.0;
                // calculate empiric risk:
                for (int k = 0; k < nl; k++) {
                    double devi = Math.abs(approx.getValue(k) - yml.getValue(k)) - param.p;
                    if (devi < 0.0)
                        devi = 0.0;
                    BEC += devi;
                }
                BEC *= param.C;
                double stabil = 0.0;
                for (int k = 0; k < w.length; k++) {
                    stabil += w[k]*w[k];
                }
                BEC += 0.5*stabil;
                // add integral offset:
                BEC -= nl*Math.log(param.C/(2*(1+param.C*param.p)));
                conti.bec_info[size*j+i] = BEC;

                b += db;
            }
            e += de;
        }
               /*
        FileWriter fmse = null;
        FileWriter fbec = null;
        FileWriter feps = null;
        FileWriter fbet = null;
        try {
            fmse = new FileWriter("mse.txt");
            fbec = new FileWriter("bec.txt");
            feps = new FileWriter("eps.txt");
            fbet = new FileWriter("bet.txt");

            double ep = conti.e_min;
            for (int i = 0; i < size; i++) {

              double b = conti.b_min;
              for (int j = 0; j < size; j++) {
                  fmse.write(String.valueOf(conti.mse_info[size*j+i]));
                  fmse.write("\t");
                  fbec.write(String.valueOf(conti.bec_info[size*j+i]));
                  fbec.write("\t");
                  feps.write(String.valueOf(ep));
                  feps.write("\t");
                  fbet.write(String.valueOf(b));
                  fbet.write("\t");
                  b += db;
              }
                fmse.write("\n");
                fbec.write("\n");
                feps.write("\n");
                fbet.write("\n");
                ep += de;
            }
            fmse.flush();
            fmse.close();
            fbec.flush();
            fbec.close();
            feps.flush();
            feps.close();
            fbet.flush();
            fbet.close();
        } catch (IOException e1) {
            e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

                   */
        if (contour_sheet_element != null)
            resulttab.remove(contour_sheet_element);
        resulttab.add(StringResources.get(StringResources.paramcontours), contour_sheet_element = new ContoursSheet(conti));
    }

    private ifVectorData solveproblem(svm_problem prob, svm_parameter param) {
        // build model & classify
        svm_model model = svm.svm_train(prob, param);


        final int learninglength = prob.x.length;
        approx = new clVectorData(learninglength);
        // calculate model output on learning set:
        for (int i = 0; i < learninglength; i++) {
            final double prd = svm.svm_predict(model, prob.x[i]);
            approx.setValue(i, prd);
        }

        final int testinglength = xt.length;
        clVectorData predicted = new clVectorData(testinglength);
        // calculate model output on test set:
        for (int i = 0; i < testinglength; i++) {
            final double prd = svm.svm_predict(model, xt[i]);
            predicted.setValue(i, prd);
        }

        // calculate coefs:
        int nsv = model.SV.length;
        final int koef_num = xml.getRows();

        w = new double[koef_num];
        for (int j = 0; j < koef_num; j++) {
            w[j] = 0.0;
        }
        for (int i = 0; i < nsv; i++) {
            int sv_number = (int) model.SV[i][0].value;

            for (int j = 0; j < koef_num; j++) {
                w[j] += model.sv_coef[0][i]*xml.getValue(j, sv_number-1);
            }
        }

        model_bias = -model.rho[0];

        clTracer.strace("w=");
        for (int j = 0; j < koef_num; j++) {
            clTracer.strace(" "+clMathEx.formatDouble(w[j], 2));
        }
        clTracer.straceln("");
        clTracer.straceln("b="+clMathEx.formatDouble(model_bias, 2));

        return predicted;
    }

    private svm_parameter createparameters() {
        svm_parameter param = new svm_parameter();
        // default values
        param.svm_type = svm_parameter.EPSILON_SVR;
        param.kernel_type = svm_parameter.PRECOMPUTED;
        param.degree = 2;
        param.cache_size = 40;
        param.C = Double.parseDouble(beta.getText().trim());
        param.eps = 0.001;//precision
        param.nu = 0.1;
        param.p = Double.parseDouble(epsilon.getText().trim());
        param.shrinking = 0;
        param.probability = 0;
        param.gamma = 1;

        return param;
    }

    private svm_problem createproblem() {
        // build problem:
		svm_problem prob = new svm_problem();
        xml = BuildExpansion(dataprovider.GetLearningInputData());
        yml = dataprovider.GetLearningOutputData();
        final int nfac = xml.getRows();
		prob.l = xml.getCols();
		prob.y = new double[prob.l];

        prob.x = new svm_node[prob.l][prob.l+1];
        for(int i=0; i<prob.l; i++)
        {
            prob.x[i][0] = new svm_node();
            prob.x[i][0].value = i+1;
            prob.x[i][0].index = 0;

            for(int j=1; j<prob.l+1; j++)
            {
                double dot_value = 0.0;
                for(int k=0; k<nfac; k++)
                    dot_value += xml.getValue(k,i)*xml.getValue(k,j-1);

                prob.x[i][j] = new svm_node();
                prob.x[i][j].index = 0;
                prob.x[i][j].value = dot_value;
            }
            prob.y[i] = yml.getValue(i);
        }

        // prepare test data :
        final ifMatrixData xmt = BuildExpansion(dataprovider.GetTestingInputData());
        ymt = dataprovider.GetTestingOutputData();

        final int testinglength = xmt.getCols();

        xt = new svm_node[testinglength][prob.l+1];
        for (int i = 0; i < testinglength; i++)
            for(int j=0; j<prob.l+1; j++)
                xt[i][j] = new svm_node();

        for (int i = 0; i < testinglength; i++)
        {
            xt[i][0].value = i+1;
            xt[i][0].index = 0;

            for(int j=1; j<prob.l+1; j++)
            {
                double dot_value = 0.0;
                for(int k=0; k<nfac; k++)
                {
                    dot_value += xmt.getValue(k,i)*xml.getValue(k,j-1);
                }
                xt[i][j].index = 0;
                xt[i][j].value = dot_value;

            }
        }

        return prob;
    }


    private void runPRIAMonce() {

        svm_problem prob = createproblem();
        svm_parameter param = createparameters();
        ifVectorData predicted = solveproblem(prob, param);

        resulthandler.SetPredictedData(predicted, approx);

        if (individuals_sheet_element != null )
            resulttab.remove(individuals_sheet_element);
        resulttab.add(StringResources.get(StringResources.individualdep), individuals_sheet_element = new IndividualsSheet(dataprovider, w, model_bias));
    }

    public static ifMatrixData BuildExpansion(ifMatrixData input) {

        final int nfactors = input.getRows();
        final int length = input.getCols();
        final int polyorder = 1;
        ifBernsteinPack BernsteinPack = new clUniBernsteinPolyPack(polyorder);
        ifCasteljau Casteljau = new clUniCasteljau(polyorder);

        final int nexpanded = BernsteinPack.getPolyDim()*nfactors;

        final ifMatrixData cMatrixData = new clMatrixData(nexpanded, length);

        double[] vbar = new double[1];
        double[] vpoly = new double[BernsteinPack.getPolyDim()];
        double[] vec = new double[1];

        final double [] min = {0.0};
        final double [] max = {1.0};
        try {
            Casteljau.setKnotsUniformly(min, max);
            for (int i = 0; i < length; i++) {

                for (int j = 0; j < nfactors; j++) {

                    vec[0] = input.getValue(j, i);
                    Casteljau.mapUsingFastAccess(vec, vbar);
                    try {
                        BernsteinPack.getVectorOnBarycentric(vbar, vpoly);
                    } catch (clPolynomialException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }

                    cMatrixData.setValue(2*j, i, vpoly[0]);
                    cMatrixData.setValue(2*j+1, i, vpoly[1]);

                }
            }

        } catch (clCasteljauException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        return cMatrixData;
    }

}

final class contourinfo {
    public double e_min;
    public double e_max;
    public double b_min;
    public double b_max;
    public int n_e;
    public int n_b;
    double [] mse_info;
    public double[] bec_info;
}

class FormatWithPrecision implements ifCustomFormat {
    final private int precision;

    public FormatWithPrecision(int precision) {
        this.precision = precision;
    }

    public String formatDouble(double value) {
        return clMathEx.formatDouble(value, precision);
    }
}

final class ContoursSheet extends JPanel {

    public ContoursSheet(contourinfo conti) {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        Contour graph_mse = new Contour();
        graph_mse.setDataBackground(new Color(0.933f,0.914f,0.749f));
        graph_mse.setContourColor(new Color(0.180f,0.545f,0.341f));
        graph_mse.setLabelledContourColor(new Color(0.5f,.0f,0.0f));

        graph_mse.setLabelPrecision(2);
        graph_mse.setLabelSignificance(3);
        graph_mse.setCustomFormat(new FormatWithPrecision(3));

        graph_mse.setGrid(conti.mse_info, conti.n_e, conti.n_b);
        graph_mse.setRange(conti.e_min, conti.e_max, conti.b_min, conti.b_max);
        graph_mse.setLimitsToGrid(true);
        graph_mse.setLabelLevels(1);
        graph_mse.setNLevels(10);

        graph_mse.setFont(new Font("TimesRoman",Font.PLAIN,20));
        Label title = new Label(StringResources.get(StringResources.MSECountours), Label.CENTER);
        title.setFont(new Font("TimesRoman",Font.PLAIN,22));

        JPanel leftp = new JPanel();
        leftp.setLayout( new BorderLayout() );
        //leftp.setPreferredSize(new Dimension(200, 200));
        leftp.add(graph_mse, BorderLayout.CENTER);
        leftp.add(title, BorderLayout.NORTH);
        add(leftp);

        Axis xaxis = graph_mse.createXAxis();
        xaxis.setTitleText(StringResources.get(StringResources.eps_parameter));
        xaxis.setTitleColor(Color.black);
        xaxis.setTitleFont(new Font("TimesRoman",Font.PLAIN,20));
        xaxis.setLabelFont(new Font("Helvetica",Font.PLAIN,15));
        xaxis.setCustomLabelFormat(new FormatWithPrecision(2));

        Axis yaxis = graph_mse.createYAxis();
        yaxis.setTitleText(StringResources.get(StringResources.beta_parameter));
        yaxis.setTitleColor(Color.black);
        yaxis.setTitleFont(new Font("TimesRoman",Font.PLAIN,20));
        yaxis.setLabelFont(new Font("Helvetica",Font.PLAIN,15));
        yaxis.setCustomLabelFormat(new FormatWithPrecision(1));

        Contour graph_bec = new Contour();
        graph_bec.setDataBackground(new Color(0.933f,0.914f,0.749f));
        graph_bec.setContourColor(new Color(0.180f,0.545f,0.341f));
        graph_bec.setLabelledContourColor(new Color(0.5f,.0f,0.0f));

        graph_bec.setLabelPrecision(0);
        graph_bec.setLabelSignificance(2);
        graph_bec.setCustomFormat(new FormatWithPrecision(1));

        graph_bec.setGrid(conti.bec_info, conti.n_e, conti.n_b);
        graph_bec.setRange(conti.e_min, conti.e_max, conti.b_min, conti.b_max);
        graph_bec.setLimitsToGrid(true);
        graph_bec.setLabelLevels(1);
        graph_bec.setNLevels(10);
        //graph_bec.setLabelStyle(TextLine.SCIENTIFIC);

        graph_bec.setFont(new Font("TimesRoman",Font.PLAIN,20));
        Label title_bec = new Label(StringResources.get(StringResources.BECCountours), Label.CENTER);
        title_bec.setFont(new Font("TimesRoman",Font.PLAIN,22));

        JPanel rightp = new JPanel();
        rightp.setLayout( new BorderLayout() );
        //rightp.setPreferredSize(new Dimension(200, 200));
        rightp.add(graph_bec, BorderLayout.CENTER);
        rightp.add(title_bec, BorderLayout.NORTH);
        add(rightp);

        Axis xaxis_bec = graph_bec.createXAxis();
        xaxis_bec.setTitleText(StringResources.get(StringResources.eps_parameter));
        xaxis_bec.setTitleColor(Color.black);
        xaxis_bec.setTitleFont(new Font("TimesRoman",Font.PLAIN,20));
        xaxis_bec.setLabelFont(new Font("Helvetica",Font.PLAIN,15));
        xaxis_bec.setCustomLabelFormat(new FormatWithPrecision(2));

        Axis yaxis_bec = graph_bec.createYAxis();
        yaxis_bec.setTitleText(StringResources.get(StringResources.beta_parameter));
        yaxis_bec.setTitleColor(Color.black);
        yaxis_bec.setTitleFont(new Font("TimesRoman",Font.PLAIN,20));
        yaxis_bec.setLabelFont(new Font("Helvetica",Font.PLAIN,15));
        yaxis_bec.setCustomLabelFormat(new FormatWithPrecision(1));

        this.updateUI();
      }

}

final class IndividualsSheet extends JPanel {

    public IndividualsSheet(final iDataHandler data_handler, final double[] ind_koefs, double model_bias) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        final int nfactors = data_handler.GetLearningInputData().getRows();

        // prepare synthetic data:
        final int pcount = 100;
        ifMatrixData synt = new clMatrixData(nfactors, pcount);
        for (int i = 0; i < nfactors; ++i) {
            for (int j = 0; j < pcount; ++j) {
                synt.setValue(i, j, (double)j/pcount);
            }
        }

        ifMatrixData synt_exp = PRIAMAlgoHandler.BuildExpansion(synt);

        final double ymax = data_handler.GetOutMax();
        final double ymin = data_handler.GetOutMin();

        for (int i = 0; i < nfactors; ++i) {
            double [] synt_out = new double [2*pcount];
            for (int j = 0; j < pcount; ++j) {
                synt_out[2*j] = synt.getValue(i, j);
                synt_out[2*j+1] = model_bias + ind_koefs[2*i]*synt_exp.getValue(2*i, j) + ind_koefs[2*i+1]*synt_exp.getValue(2*i+1, j);
                synt_out[2*j+1] = synt_out[2*j+1]*(ymax - ymin) + ymin;
            }

            JPanel p = new JPanel();
            p.setLayout( new BorderLayout() );
            Graph2D graph = new Graph2D();
            graph.drawzero = false;
            graph.drawgrid = false;
            graph.borderTop = 50;
            DataSet pred_dataset = graph.loadDataSet(synt_out, pcount);
            pred_dataset.linecolor = Color.red;
            //pred_dataset.legendFont(new Font("Helvetica",Font.PLAIN,15));
            //pred_dataset.legend(200, 60, StringResources.get(StringResources.predcurve);

            Axis xaxis = graph.createAxis(Axis.BOTTOM);
            xaxis.attachDataSet(pred_dataset);

            Axis yaxis = graph.createAxis(Axis.LEFT);
            yaxis.attachDataSet(pred_dataset);

            //xaxis.setTitleText(StringResources.get(StringResources.testingsamples);
            //xaxis.setTitleFont(new Font("TimesRoman",Font.PLAIN,20));
            xaxis.setLabelFont(new Font("Helvetica",Font.PLAIN,15));
            xaxis.setCustomLabelFormat(new FormatWithPrecision(2));

            yaxis.setTitleText(StringResources.get(StringResources.output));
            yaxis.setTitleFont(new Font("TimesRoman",Font.PLAIN,20));
            yaxis.setLabelFont(new Font("Helvetica",Font.PLAIN,15));
            yaxis.setCustomLabelFormat(new FormatWithPrecision(2));
            yaxis.maximum = ymax;
            yaxis.minimum = ymin;

            p.add(graph, BorderLayout.CENTER);
            //p.add(title, BorderLayout.NORTH);
            add(p);
        }

        this.updateUI();
      }


}