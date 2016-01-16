
import pm.ifPortHandler;
import pm.clPlain2DTwinPort;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.*;
import java.applet.AppletStub;
import java.applet.AppletContext;
import java.net.URL;
import java.util.Vector;
import java.io.*;

import data.ifMatrixData;
import data.clMatrixData;
import data.ifVectorData;
import data.clVectorData;
import regr.clMatrix;
import util.clTracer;
import util.clMathEx;
import util.clURL;
import graph.*;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 31.03.2007
 * Time: 15:49:35
 * To change this template use File | Settings | File Templates.
 */
public final class apple extends JApplet {

    public void init() {
        super.init();    //To change body of overridden methods use File | Settings | File Templates.
        String locale = getParameter("locale");
        if (locale != null)
            StringResources.g_locale = locale;

        final String path = getParameter("sourceData");
        final Container co = getContentPane();
        co.add(new MainLayerCtrl(path, getParameter("sourceDataList")));
    }

    public static void main(String[] argv)
    {
        new AppleFrame(StringResources.get(StringResources.appletname), new apple(), 800, 600);
    }
}

class AppleFrame extends JFrame {
	AppleFrame(String title, JApplet applet, int width, int height)
	{
		super(title);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
        applet.setStub(new AppleStub());
		applet.init();
		applet.start();
		this.getContentPane().add(applet);
        this.setSize(width,height);
		this.setVisible(true);
        this.setTitle(StringResources.get(StringResources.appletname));
	}
}

class AppleStub implements AppletStub {

    public boolean isActive() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void appletResize(int width, int height) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public AppletContext getAppletContext() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public URL getCodeBase() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public URL getDocumentBase() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getParameter(String name) {
        if (name.equals("sourceDataList"))
            return ".\\";
        if (name.equals("sourceData"))
            return ".\\_data_\\";
        if (name.equals("locale"))
            return "uk";
        return null;
    }

}

final class MainLayerCtrl extends JPanel implements ActionListener {
    private final JLabel m_Problems;
    private final JComboBox m_cProblemSelector;
    private final JLabel m_Algos;
    private final JComboBox m_cAlgorithmSelector;
    private JTabbedPane m_TaskPane = null;
    private String m_path;

    public MainLayerCtrl(String path, String list_path) {
        m_path = path;
        final Vector data_list = (new listpreloader(list_path + "_list_")).getList();
        final Vector algo_list = (new listpreloader(list_path + "_algolist_")).getList();

        setLayout(new BorderLayout());
        m_cAlgorithmSelector = new JComboBox(algo_list);
        m_cProblemSelector = new JComboBox(data_list);
        m_Problems = new JLabel(StringResources.get(StringResources.problems));
        m_Algos = new JLabel(StringResources.get(StringResources.algos));
        JPanel p0 = new JPanel();
        p0.setLayout(new FlowLayout(FlowLayout.LEFT));
        p0.add(m_Problems);
        p0.add(m_cProblemSelector);
        p0.add(m_Algos);
        p0.add(m_cAlgorithmSelector);
        final JButton bload = new JButton(StringResources.get(StringResources.loadtask));
        p0.add(bload);
        bload.addActionListener(this);
        final JButton bclear = new JButton(StringResources.get(StringResources.clearall));
        p0.add(bclear);
        bclear.addActionListener(this);
        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p1.add(m_TaskPane = new JTabbedPane(JTabbedPane.TOP));
        add(p0, BorderLayout.NORTH);
        add(p1, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        final String label = e.getActionCommand();
        if (label.equals(StringResources.get(StringResources.loadtask))) {
            final String problem = (String)m_cProblemSelector.getSelectedItem();
            final String algo = (String)m_cAlgorithmSelector.getSelectedItem();
            final BranchedTask task = new BranchedTask(m_path + problem, algo);
            m_TaskPane.add(problem + " || " + algo, task);
            m_TaskPane.setSelectedIndex(m_TaskPane.getTabCount() - 1);
            new Thread(task, "Branched task thread").start();
        } else if (label.equals(StringResources.get(StringResources.clearall))) {
            m_TaskPane.removeAll();
        }
    }
}

final class BranchedTask extends JPanel implements Runnable, ActionListener {
    final private String m_problem;
    final private String m_algo;
    private iAlgoHandler algo_handler = null;
    private iDataHandler data_handler = null;
    private iResultHandler result_handler = null;
    private JButton build_model_btn;
    private JButton cancel_model_btn;

    public BranchedTask(String problem, String algo) {
        m_problem = problem;
        m_algo = algo;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void run() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        // load data first :
        JPanel pfirstlevel = new JPanel();
        add(pfirstlevel);
        JPanel pdata = new JPanel();
        try {
            DataSheet data_sheet;
            pdata.add(data_sheet = new DataSheet(m_problem));
            data_handler = data_sheet;
        } catch (IOException e) {
            e.printStackTrace();
        }
        pfirstlevel.add(pdata);
        this.updateUI();

        // construct result flow
        JPanel psecondlevel = new JPanel();
        add(psecondlevel);
        psecondlevel.add(build_model_btn = new JButton(StringResources.get(StringResources.buildmodel)));
        build_model_btn.addActionListener(this);
        psecondlevel.add(cancel_model_btn = new JButton(StringResources.get(StringResources.cancelbuild)));
        cancel_model_btn.addActionListener(this);

        ResultSheet resultlevel = new ResultSheet(data_handler);
        JTabbedPane resulttab = new JTabbedPane(JTabbedPane.TOP);
        add(resulttab);
        resulttab.add(StringResources.get(StringResources.mainresult), resultlevel);
        resulttab.setSelectedIndex(0);

        result_handler = resultlevel;
        this.updateUI();

        // construct algorithm flow:
        if (m_algo.equals(AlgoNames.SVRAlgo)) {
            algo_handler = new SVRAlgoHandler(data_handler, result_handler);
        }else if (m_algo.equals(AlgoNames.PRIAMAlgo)) {
            algo_handler = new PRIAMAlgoHandler(data_handler, result_handler, resulttab);
        }

        JPanel palgo = new JPanel();
        pfirstlevel.add(palgo);
        algo_handler.addParameters(palgo);
        this.updateUI();

        // organize main work loop:
        while (true) {
            try {
                build_model_btn.setEnabled(true);
                cancel_model_btn.setEnabled(false);
                synchronized (algo_handler) {
                    algo_handler.wait();
                    //clTracer.strace(".Wait has passed..");
                }
            } catch (InterruptedException e) {
                clTracer.strace("Finish branched task thread: " + e.toString());
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        final String label = e.getActionCommand();
        if (label.equals(StringResources.get(StringResources.buildmodel))) {
            (new Thread(algo_handler, "Algorithm thread.")).start();
            build_model_btn.setEnabled(false);
            cancel_model_btn.setEnabled(true);
        } else if (label.equals(StringResources.get(StringResources.cancelbuild))) {
            algo_handler.cancelBuild();
        }
    }
}

final class ResultSheet extends JPanel implements iResultHandler {
    private iDataHandler data_handler;
    private Graph2D graph = null;

    public ResultSheet(iDataHandler data_handler) {
        this.data_handler = data_handler;
        setLayout( new BorderLayout() );
        setPreferredSize(new Dimension(300, 300));
    }

    private void ShowDurbinWatson(ifVectorData approx)
    {
        final ifVectorData real = data_handler.GetLearningOutputData();
        int learninglength = approx.getArraySize();
        clVectorData e = new clVectorData(learninglength);
        for (int i = 0; i < learninglength; i++) {
            e.setValue(i, real.getValue(i) - approx.getValue(i));
        }
        // calculate Durbin-Watson statistics (DW):
        double denominator = 0.0;
        for (int i = 0; i < learninglength; i++) {
            denominator += e.getValue(i)*e.getValue(i);
        }
        double DW = 0.0;
        for (int i = 1; i < learninglength; i++) {
            DW += (e.getValue(i) - e.getValue(i-1))*(e.getValue(i) - e.getValue(i-1));
        }
        DW = DW/denominator;
        clTracer.straceln("Durbin-Watson = " + clMathEx.formatDouble(DW, 5));
    }

    private void ShowTheilAccuracy(ifVectorData pred_data)
    {
        final ifVectorData real = data_handler.GetTestingOutputData();
        int testinglength = pred_data.getArraySize();
        // calculate squared error (SE):
        double SE = 0.0;
        for (int i = 0; i < testinglength; i++) {
            SE += (pred_data.getValue(i) - real.getValue(i))*(pred_data.getValue(i) - real.getValue(i));
        }
        // calculate squared real:
        double R2 = 0.0;
        for (int i = 0; i < testinglength; i++) {
            R2 += real.getValue(i)*real.getValue(i);
        }
        // calculate squared predicted:
        double P2 = 0.0;
        for (int i = 0; i < testinglength; i++) {
            P2 += pred_data.getValue(i)*pred_data.getValue(i);
        }

        clTracer.straceln("Theil accuracy = " + clMathEx.formatDouble(Math.sqrt(SE)/(Math.sqrt(R2)+Math.sqrt(P2)), 5));
    }

    public void SetPredictedData(ifVectorData pred_data, ifVectorData approx) {
        int testinglength = pred_data.getArraySize();
        ifVectorData predicted = new clVectorData(testinglength);

        final ifVectorData ymt = data_handler.GetTestingOutputData();
        clVectorData real = new clVectorData(testinglength);

        final double ymax = data_handler.GetOutMax();
        final double ymin = data_handler.GetOutMin();
        clTracer.straceln("max:" + clMathEx.formatDouble(ymax, 3) + "; min:" + clMathEx.formatDouble(ymin, 3));
        for (int i = 0; i < testinglength; i++) {
            predicted.setValue(i, pred_data.getValue(i) * (ymax - ymin) + ymin);
            real.setValue(i, ymt.getValue(i) * (ymax - ymin) + ymin);
            //clTracer.straceln(clMathEx.formatDouble(predicted.getValue(i), 3));
        }

        // calculate MSE:
        double MSE = 0.0;
        for (int i = 0; i < testinglength; i++) {
            //clTracer.straceln(clMathEx.formatDouble(predicted.getValue(i), 3));
            MSE += (predicted.getValue(i) - real.getValue(i))*(predicted.getValue(i) - real.getValue(i));
        }
        MSE /= testinglength;
        clTracer.straceln("MSEn = " + clMathEx.formatDouble(MSE/((ymax - ymin)*(ymax - ymin)), 5));

        ShowTheilAccuracy(pred_data);
        ShowDurbinWatson(approx);
        //if (graph != null)
        //    remove(graph);
        this.removeAll();

        graph = new Graph2D();
        graph.drawzero = false;
        graph.drawgrid = false;
        graph.borderTop = 50;

        add(graph, BorderLayout.CENTER);
        JPanel add_info = new JPanel();
        add_info.add(new JLabel(StringResources.get(StringResources.MSEInfo) + clMathEx.formatDouble(MSE, 5)));
        add(add_info, BorderLayout.WEST);

        double [] pred_dataarray = new double[2*testinglength];
        for (int i = 0; i < testinglength; i++) {
            pred_dataarray[2*i] = i;
            pred_dataarray[2*i+1] = predicted.getValue(i);
        }

        double [] real_dataarray = new double[2*testinglength];
        for (int i = 0; i < testinglength; i++) {
            real_dataarray[2*i] = i;
            real_dataarray[2*i+1] = real.getValue(i);
        }

        DataSet pred_dataset = graph.loadDataSet(pred_dataarray, testinglength);
        pred_dataset.linecolor = Color.red;
        pred_dataset.legendFont(new Font("Helvetica",Font.PLAIN,15));
        pred_dataset.legend(200, 60, StringResources.get(StringResources.predcurve));
        DataSet real_dataset = graph.loadDataSet(real_dataarray, testinglength);
        real_dataset.linecolor = Color.blue;
        real_dataset.legendFont(new Font("Helvetica",Font.PLAIN,15));
        real_dataset.legend(100, 60, StringResources.get(StringResources.realcurve));


        Axis xaxis = graph.createAxis(Axis.BOTTOM);
        xaxis.attachDataSet(pred_dataset);
        xaxis.attachDataSet(real_dataset);
        Axis yaxis = graph.createAxis(Axis.LEFT);
        yaxis.attachDataSet(pred_dataset);
        yaxis.attachDataSet(real_dataset);

        xaxis.setTitleText(StringResources.get(StringResources.testingsamples));
        xaxis.setTitleFont(new Font("TimesRoman",Font.PLAIN,20));
        xaxis.setLabelFont(new Font("Helvetica",Font.PLAIN,15));

        yaxis.setTitleText(StringResources.get(StringResources.output));
        yaxis.setTitleFont(new Font("TimesRoman",Font.PLAIN,20));
        yaxis.setLabelFont(new Font("Helvetica",Font.PLAIN,15));

        this.updateUI();
      }

}

final class DataSheet extends JPanel implements iDataHandler {
    final private ifMatrixData xm_tn;
    final private ifMatrixData ym_tn;
    final private int datalength;
    final private int numoffac;
    final private JSpinner lw_start;
    final private JSpinner lw_end;
    final private JSpinner tw_start;
    final private JSpinner tw_end;
    final private double ymin;
    final private double ymax;

    private JCheckBox outch_;
    private JCheckBox[] inch_;

    public DataSheet(final String problem) throws IOException {
        setLayout(new GridLayout(3,1,0,0));
        final ifPortHandler p;
        String correct_filename = clPlain2DTwinPort.CorrectFileName(problem);
        if (clURL.isURL(correct_filename))
                p = new clPlain2DTwinPort(new URL(correct_filename));
            else
                p = new clPlain2DTwinPort(correct_filename);

        final ifMatrixData xm = p.getData(0);
        final ifMatrixData ym = p.getData(1);
        final String[] descr = p.getDataDesc();
        final String problem_descr = p.getProblemDesc();

        JPanel pinfo0 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pinfo0.add(new JLabel(StringResources.get(StringResources.problemdescription)+problem_descr));
        add(pinfo0);

        xm_tn = (new clMatrix(xm)).transpose();
        ym_tn = (new clMatrix(ym)).transpose();
        ymin = ym_tn.getVectorPtr(0).getMin();
        ymax = ym_tn.getVectorPtr(0).getMax();
        xm_tn.normalize(0,1);
        ym_tn.normalize(0,1);
        JPanel pinfo1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pinfo1.add(new JLabel(StringResources.get(StringResources.datalength) + Integer.toString(datalength = xm.getRows()) + "; "));
        pinfo1.add(new JLabel(StringResources.get(StringResources.learningwindow)));
        pinfo1.add(lw_start = new JSpinner());
        final SpinnerNumberModel start_model_l;
        lw_start.setModel(start_model_l = new SpinnerNumberModel(1,1,datalength-1,1));
        pinfo1.add(new JLabel(" " + StringResources.get(StringResources.to) + " "));
        pinfo1.add(lw_end = new JSpinner());
        final SpinnerNumberModel end_model_l;
        lw_end.setModel(end_model_l = new SpinnerNumberModel(datalength,2,datalength,1));
        // set some restrictions :
        end_model_l.setMinimum(new lowerbounddetector(start_model_l));
        start_model_l.setMaximum(new upperbounddetector(end_model_l));
        pinfo1.add(new JLabel(StringResources.get(StringResources.testingwindow)));
        pinfo1.add(tw_start = new JSpinner());
        final SpinnerNumberModel start_model_t;
        tw_start.setModel(start_model_t = new SpinnerNumberModel(1,1,datalength-1,1));
        pinfo1.add(new JLabel(" " + StringResources.get(StringResources.to) + " "));
        pinfo1.add(tw_end = new JSpinner());
        final SpinnerNumberModel end_model_t;
        tw_end.setModel(end_model_t = new SpinnerNumberModel(datalength,2,datalength,1));
        // set some restrictions :
        end_model_t.setMinimum(new lowerbounddetector(start_model_t));
        start_model_t.setMaximum(new upperbounddetector(end_model_t));
        add(pinfo1);

        JPanel pinfo2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        numoffac = xm.getCols();
        pinfo2.add(new JLabel(StringResources.get(StringResources.OutputFactor)));
        add(pinfo2);
        if (descr.length > numoffac)
        {
            pinfo2.add(outch_ = new JCheckBox(descr[numoffac], true));
            outch_.setEnabled(false);
        }
        pinfo2.add(new JLabel(StringResources.get(StringResources.InputFactors)));
        inch_ = new JCheckBox[numoffac];
        for (int i = 0; i< numoffac; i++) {
            pinfo2.add(inch_[i] = new JCheckBox(descr[i], true));

        }
    }

    private ifMatrixData _GetInputData(final int lstart, final int lend) {
        int activeinput = 0;
        for (int i = 0; i< numoffac; i++) {
            if (inch_[i].isSelected())
                activeinput++;
        }

        clMatrixData result = new clMatrixData(activeinput, lend - lstart + 1);
        for (int i = 0, a = 0; i< numoffac; i++) {
            if (!inch_[i].isSelected())
                continue;
            for (int j = lstart; j <= lend; j++) {
                final double d = xm_tn.getValue(i, j-1);
                result.setValue(a, j - lstart, d);
            }
            a++; // actual factor
        }
        return result;
    }

    private ifVectorData _GetOutputData(final int lstart, final int lend) {

        clVectorData result = new clVectorData(lend - lstart + 1);
        for (int j = lstart; j <= lend; j++) {
            final double d = ym_tn.getValue(0, j-1);
            result.setValue(j - lstart, d);
        }
        return result;
    }

    public ifMatrixData GetLearningInputData() {
        int lstart = ((Number)lw_start.getValue()).intValue();
        int lend = ((Number)lw_end.getValue()).intValue();
        return _GetInputData(lstart, lend);
    }

    public ifMatrixData GetTestingInputData() {
        int lstart = ((Number)tw_start.getValue()).intValue();
        int lend = ((Number)tw_end.getValue()).intValue();
        return _GetInputData(lstart, lend);
    }

    public ifVectorData GetLearningOutputData() {
        int lstart = ((Number)lw_start.getValue()).intValue();
        int lend = ((Number)lw_end.getValue()).intValue();
        return _GetOutputData(lstart, lend);
    }

    public ifVectorData GetTestingOutputData() {
        int lstart = ((Number)tw_start.getValue()).intValue();
        int lend = ((Number)tw_end.getValue()).intValue();
        return _GetOutputData(lstart, lend);
    }

    public double GetOutMin() {
        return ymin;
    }

    public double GetOutMax() {
        return ymax;
    }

}

final class lowerbounddetector implements Comparable {
    final private SpinnerNumberModel start_model;

    lowerbounddetector(SpinnerNumberModel start_model) {
        this.start_model = start_model;
    }

    public int compareTo(Object o) {
        return 1+start_model.getNumber().intValue() - ((Number)o).intValue();
    }
}

final class upperbounddetector implements Comparable {
    final private SpinnerNumberModel end_model;

    upperbounddetector(SpinnerNumberModel end_model) {
        this.end_model = end_model;
    }

    public int compareTo(Object o) {
        return -1 + end_model.getNumber().intValue() - ((Number)o).intValue();
    }
}
