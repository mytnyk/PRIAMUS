
import data.ifVectorData;
import data.ifMatrixData;

import javax.swing.*;

interface iResultHandler {

    void SetPredictedData(ifVectorData data, ifVectorData approx);
}

interface iDataHandler {

    ifMatrixData GetLearningInputData();

    ifMatrixData GetTestingInputData();

    ifVectorData GetLearningOutputData();

    ifVectorData GetTestingOutputData();

    double GetOutMin();

    double GetOutMax();
}

interface iAlgoHandler extends Runnable {

    void addParameters(JPanel palgo);

    void cancelBuild();
}