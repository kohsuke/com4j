import excel._Application;
import excel._Workbook;
import excel._Worksheet;
import com4j.Variant;

/**
 * @author Kohsuke Kawaguchi
 */
public class ExcelDemo {
    public static void main(String[] args) {
        _Application app = excel.ClassFactory.createApplication();
        app.setVisible(0,true);
        app.getWorkbooks().add(null, 0);
        app.getActiveCell().setFormulaR1C1("Welcome to com4j");
        //_Worksheet sheet = workbook.worksheets(1).queryInterface(_Worksheet.class);
        //
        //sheet.cells()._Default(1,1,"Welcome to com4j!");
    }
}
