package wmi;

import com4j.Com4jObject;

/**
 * Uses Microsoft WMI Scripting Library to access the system information.
 *
 * <p>
 * For more about WMI, see
 *
 * <ul>
 *  <li>http://msdn.microsoft.com/library/default.asp?url=/library/en-us/dnanchor/html/anch_wmi.asp
 *  <li>http://msdn.microsoft.com/library/default.asp?url=/library/en-us/wmisdk/wmi/scripting_api_objects.asp
 *  <li>http://www.microsoft.com/downloads/details.aspx?FamilyId=6430F853-1120-48DB-8CC5-F2ABDC3ED314&displaylang=en
 * </ul>
 *
 * <p>
 * Thanks to Simon Assens for pointing me to WMI.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Connecting to WMI repository");
        ISWbemLocator wbemLocator = ClassFactory.createSWbemLocator();
        ISWbemNamedValueSet nvs = ClassFactory.createSWbemNamedValueSet();
        // connecting to WMI repository
        ISWbemServices wbemServices = wbemLocator.connectServer("localhost","Root\\CIMv2","","","","",0,nvs);
        System.out.println("connected");



        // gathering all 'System' log events
        System.out.println("Listing logical disks");
        ISWbemObjectSet result = wbemServices.execQuery("Select * from Win32_LogicalDisk","WQL",16,nvs);

        for( Com4jObject obj : result ) {
            ISWbemObject wo = obj.queryInterface(ISWbemObject.class);
            System.out.println(wo.getObjectText_(0));
//            Object o = wo.properties_().item("Description", 0).value();
//            System.out.println(o);
        }
    }
}
