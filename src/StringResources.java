
import java.awt.*;

final class AlgoNames {
    public static String SVRAlgo = "SVR"; // Should not be translated
    public static String PRIAMAlgo = "PRIAM"; // Should not be translated
}

final class StringResources {

    public static String appletname = "*PRIAMUS modeling tool";
    public static String problems = "*List of problems:";
    public static String algos = "*List of algorithms:";
    public static String loadtask = "*LOAD TASK";
    public static String clearall = "*CLEAR ALL";
    public static String buildmodel = "*BUILD MODEL";
    public static String cancelbuild = "*CANCEL BUILD";
    public static String MSEInfo = "*MSE = ";
    public static String predcurve = "*predicted";
    public static String realcurve = "*real";
    public static String output = "*output";
    public static String testingsamples = "*samples from testing window";
    public static String datalength = "*Data length = ";
    public static String learningwindow = "*Learning window from ";
    public static String testingwindow = "*Testing window from ";
    public static String OutputFactor = "*Output factor: ";
    public static String InputFactors = "*Input factors: ";
    public static String eps_parameter = "*epsilon";
    public static String beta_parameter = "*beta";
    public static String to = "*to";
    public static String mainresult = "*Model";
    public static String bruteforce = "*Brute force";
    public static String paramcontours = "*Contours";
    public static String BECCountours = "*BEC contours";
    public static String MSECountours = "*MSE contours";
    public static String individualdep = "*Individual dependencies";
    public static String problemdescription = "*Information about model: ";

    public static String g_locale = "en";
    public static String get(String name) {
        if (g_locale.equals("en"))
        {
            return getEnglish(name);
        }
        else if (g_locale.equals("uk"))
        {
            return getUkrainian(name);
        }
        else
        {
            return name;
        }
    }

    private static String getEnglish(String name) {
        final String[][] content = {
      { appletname, "PRIAMUS modeling tool" },
      { problems , "List of problems:" } ,
      { algos, "List of algorithms:" } ,
      { loadtask  , "LOAD TASK"},
      { clearall  , "CLEAR ALL" },
      { buildmodel  , "BUILD MODEL"},
      { cancelbuild  , "CANCEL BUILD"},
      { MSEInfo  , "MSE = "},
      { predcurve  , "predicted"},
      { realcurve  , "real"},
      { output  , "output"},
      { testingsamples  , "samples from testing window"},
      { datalength  , "Data length = "},
      { learningwindow  , "Learning window from "},
      { testingwindow  , "Testing window from "},
      { OutputFactor  , "Output factor: "},
      { InputFactors  , "Input factors: "},
      { eps_parameter  , "epsilon"},
      { beta_parameter  , "beta"},
      { to  , "to"},
      { mainresult  , "Model"},
      { bruteforce  , "Brute force"},
      { paramcontours  , "Contours"},
      { BECCountours  , "BEC contours"},
      { MSECountours  , "MSE contours"},
      { individualdep  , "Individual dependencies"},
      { problemdescription  , "Information about model: "},
        };
        for (int k = 0;  k < content.length; k++) {
        if (content[k][0] == name)
            return content[k][1];
        }
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    private static String getUkrainian(String name) {
      final String[][] content = {
      { appletname, "ПРІАМ" },
      { problems , "Список задач:" } ,
      { algos, "Список алгоритмів:" } ,
      { loadtask  , "ЗАВАНТАЖИТИ"},
      { clearall  , "ВИДАЛИТИ" },
      { buildmodel  , "БУДУВАТИ МОДЕЛЬ"},
      { cancelbuild  , "ВІДМІНИТИ"},
      { MSEInfo  , "СКП = "},
      { predcurve  , "спрогнозовані"},
      { realcurve  , "реальні"},
      { output  , "вихід"},
      { testingsamples  , "елементи вибірки для тестування"},
      { datalength  , "Довжина вибірки = "},
      { learningwindow  , "Вікно навчання з "},
      { testingwindow  , "Вікно тестування з "},
      { OutputFactor  , "Вихідний фактор: "},
      { InputFactors  , "Вхідні фактори: "},
      { eps_parameter  , "епсілон"},
      { beta_parameter  , "бета"},
      { to  , "до"},
      { mainresult  , "Модель"},
      { bruteforce  , "Перебір"},
      { paramcontours  , "Контури"},
      { BECCountours  , "Контури КБП"},
      { MSECountours  , "Контури СКП"},
      { individualdep  , "Індивідуальні залежності"},
      { problemdescription  , "Відомості про модель: "},
        };
        for (int k = 0;  k < content.length; k++) {
        if (content[k][0] == name)
            return content[k][1];
        }
        return null;  //To change body of created methods use File | Settings | File Templates.
    }
}