
import libsvm.*;

import javax.swing.*;
import java.awt.*;

import data.ifMatrixData;
import data.ifVectorData;
import data.clVectorData;

class SVRAlgoHandler implements iAlgoHandler {
    private boolean m_stop = false;
    final private iDataHandler dataprovider;
    final private iResultHandler resulthandler;
    private JTextField epsilon;
    private JTextField beta;

    public SVRAlgoHandler(iDataHandler dataprovider, iResultHandler resulthandler) {
        this.dataprovider = dataprovider;
        this.resulthandler = resulthandler;
    }

    public void addParameters(JPanel palgo) {
        palgo.setLayout(new GridLayout(2,2,0,0));

        palgo.add(new JLabel(StringResources.get(StringResources.eps_parameter)+":"));
        palgo.add(epsilon = new JTextField("0.021", 3));
        palgo.add(new JLabel(StringResources.get(StringResources.beta_parameter)+":"));
        palgo.add(beta = new JTextField("7.6", 3));
    }

    public void cancelBuild() {
        m_stop = true;
    }

    public synchronized void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        m_stop = false;
        while (!m_stop) {
            runSVR();
            m_stop = true;
        }
        notify(); // notify branched thread that algo has finished
    }

    private void runSVR() {

        svm_parameter param = new svm_parameter();

        // default values
        param.svm_type = svm_parameter.EPSILON_SVR;
        param.kernel_type = svm_parameter.RBF;//svm_parameter.LINEAR;
        param.degree = 2;
        param.cache_size = 40;
        param.C = Double.parseDouble(beta.getText().trim());
        param.eps = 0.001;//precision
        param.nu = 0.1;
        param.p = Double.parseDouble(epsilon.getText().trim());
        param.shrinking = 0;
        param.probability = 0;
        param.gamma = 1;

        // build problem
		svm_problem prob = new svm_problem();
        final ifMatrixData xml = dataprovider.GetLearningInputData();
        final ifVectorData yml = dataprovider.GetLearningOutputData();
        final int nfac = xml.getRows();
		prob.l = xml.getCols();
		prob.y = new double[prob.l];

        prob.x = new svm_node[prob.l][nfac];
        for(int i=0; i<prob.l; i++)
        {
            for(int j=0; j<nfac; j++)
            {
                prob.x[i][j] = new svm_node();
                prob.x[i][j].index = j+1;
                prob.x[i][j].value = xml.getValue(j,i);
            }
            prob.y[i] = yml.getValue(i);
        }

        // build model & classify
        svm_model model = svm.svm_train(prob, param);

        // test data :
        final ifMatrixData xmt = dataprovider.GetTestingInputData();
        //final ifVectorData ymt = dataprovider.GetTestingOutputData();

        final int testinglength = xmt.getCols();
        clVectorData predicted = new clVectorData(testinglength);

        final svm_node[] xt = new svm_node[nfac];
        for(int j=0; j<nfac; j++)
            xt[j] = new svm_node();

        for (int i = 0; i < testinglength; i++)
        {
            for(int j=0; j<nfac; j++)
            {
                xt[j].index = j+1;
                xt[j].value = xmt.getValue(j,i);
            }

            final double prd = svm.svm_predict(model, xt);
            predicted.setValue(i, prd);
        }

        final int learninglength = prob.x.length;
        clVectorData approx = new clVectorData(learninglength);
        // calculate model output on learning set:
        for (int i = 0; i < learninglength; i++) {
            final double prd = svm.svm_predict(model, prob.x[i]);
            approx.setValue(i, prd);
        }

        resulthandler.SetPredictedData(predicted, approx);
    }
}
