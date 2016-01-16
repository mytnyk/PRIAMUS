
import libsvm.*;

import java.io.IOException;

import pm.ifPortHandler;
import data.ifMatrixData;
import util.clTracer;
import regr.clMatrix;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 12.11.2006
 * Time: 16:44:58
 * To change this template use File | Settings | File Templates.
 */
public class svr_run {

    public static void main(String[] argv) throws IOException {
        test_linear(argv);
        test_precomp(argv);
    }

    private static void test_precomp(String[] argv) throws IOException {
        // read data:
        final ifPortHandler p = new pm.clPlain2DTwinPort(argv[0]);
        final ifMatrixData xm = p.getData(0);
        final ifMatrixData ym = p.getData(1);

        // normilize data
        final ifMatrixData xm_t = (new clMatrix(xm)).transpose();
        final ifMatrixData ym_t = (new clMatrix(ym)).transpose();
        xm_t.normalize(0,1);
        ym_t.normalize(0,1);

        svm_parameter param = new svm_parameter();

        // default values
        param.svm_type = svm_parameter.EPSILON_SVR;
        param.kernel_type = svm_parameter.PRECOMPUTED;
        param.degree = 2;
        param.cache_size = 40;
        param.C = 1;//beta parameter
        param.eps = 0.001;//precision
        param.nu = 0.1;
        param.p = 0.01;//epsilon parameter  0.01
        param.shrinking = 0;
        param.probability = 0;
        param.gamma = 1;

        // build problem
		svm_problem prob = new svm_problem();
        final int nfac = xm.getCols();
		prob.l = xm.getRows();
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
                {
                    dot_value += xm_t.getValue(k,i)*xm_t.getValue(k,j-1);
                }
                prob.x[i][j] = new svm_node();
                prob.x[i][j].index = 0;
                prob.x[i][j].value = dot_value;

            }
            prob.y[i] = ym_t.getValue(0,i);
        }
        // build model & classify
        svm_model model = svm.svm_train(prob, param);

        double[] predicted = new double[prob.l];

        for (int i = 0; i < prob.l; i++)
        {
            predicted[i] = svm.svm_predict(model, prob.x[i]);
            clTracer.straceln(Double.toString(predicted[i]));
        }
    }

    private static void test_linear(String[] argv) throws IOException {
        // read data:
        final ifPortHandler p = new pm.clPlain2DTwinPort(argv[0]);
        final ifMatrixData xm = p.getData(0);
        final ifMatrixData ym = p.getData(1);
        //final String[] sDataDesc = p.getDataDesc();

        // normilize data
        final ifMatrixData xm_t = (new clMatrix(xm)).transpose();
        final ifMatrixData ym_t = (new clMatrix(ym)).transpose();
        xm_t.normalize(0,1);
        ym_t.normalize(0,1);

        svm_parameter param = new svm_parameter();

        // default values
        param.svm_type = svm_parameter.EPSILON_SVR;
        param.kernel_type = svm_parameter.LINEAR;
        param.degree = 2;
        param.cache_size = 40;
        param.C = 1;//beta parameter
        param.eps = 0.001;//precision
        param.nu = 0.1;
        param.p = 0.01;//epsilon parameter  0.01
        param.shrinking = 0;
        param.probability = 0;
        param.gamma = 1;

        // build problem
		svm_problem prob = new svm_problem();
        final int nfac = xm.getCols();
		prob.l = xm.getRows();
		prob.y = new double[prob.l];

        prob.x = new svm_node[prob.l][nfac];
        for(int i=0; i<prob.l; i++)
        {
        for(int j=0; j<nfac; j++)
        {
            prob.x[i][j] = new svm_node();
            prob.x[i][j].index = j+1;
            prob.x[i][j].value = xm_t.getValue(j,i);
        }
            prob.y[i] = ym_t.getValue(0,i);
        }

        // build model & classify
        svm_model model = svm.svm_train(prob, param);

        double[] predicted = new double[prob.l];

        for (int i = 0; i < prob.l; i++)
        {
            predicted[i] = svm.svm_predict(model, prob.x[i]);
            clTracer.straceln(Double.toString(predicted[i]));
        }
    }
}
