package wmi;

import com4j.Com4jObject;
import wmi.events.ISWbemSinkEvents;

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
    public static void main(String[] args) throws Exception {
        listEventLog();

        System.out.println("Connecting to WMI repository");
        ISWbemLocator wbemLocator = ClassFactory.createSWbemLocator();

        // connecting to WMI repository
        ISWbemServices wbemServices = wbemLocator.connectServer("localhost","Root\\CIMv2","","","","",0,null);
        System.out.println("connected");

        {// query - what disks do we have?
            System.out.println("Listing logical disks");
            ISWbemObjectSet result = wbemServices.execQuery("Select * from Win32_LogicalDisk","WQL",16,null);

            for( Com4jObject obj : result ) {
                ISWbemObject wo = obj.queryInterface(ISWbemObject.class);
                System.out.println(wo.getObjectText_(0));
            }
        }

        {// monitor events - what processes are being created?
            ISWbemSink sink = ClassFactory.createSWbemSink();
            sink.advise(ISWbemSinkEvents.class,new ISWbemSinkEvents() {
                public void onObjectReady(ISWbemObject wmiObject, ISWbemNamedValueSet objWbemAsyncContext) {
                    System.out.println("Received event: "+wmiObject.getObjectText_(0));
                }
            });
            wbemServices.execNotificationQueryAsync(sink,
                "SELECT * FROM __InstanceCreationEvent WITHIN 1 WHERE TargetInstance ISA 'Win32_Process'","WQL",0,null,null);

            System.out.println("waiting for process creation events");
            System.out.println("will exit in 15 seconds. Try to launch new program and see what happens");
            Thread.sleep(15000);
            System.out.println("exiting");
            sink.cancel();
        }
    }

    public static void listEventLog() throws Exception {
        System.out.println("Connecting to WMI repository");
        ISWbemLocator wbemLocator = ClassFactory.createSWbemLocator();

        // connecting to WMI repository
        ISWbemServices wbemServices = wbemLocator.connectServer("localhost","Root\\CIMv2","","","","",0,null);
        System.out.println("connected");

        ISWbemObjectSet result = wbemServices.execQuery("Select * from Win32_NTLogEvent",
            "WQL", 16, null);

        for( Com4jObject obj : result ) {
            ISWbemObject wo = obj.queryInterface(ISWbemObject.class);
            System.out.println(wo.getObjectText_(0)); // I saw EventType value is  "3"

            wo.properties_("EventType", 0); //I can write this , I didn't get Exception

            ISWbemProperty s = wo.properties_("EventType", 0); // This is ok ...

            System.out.println(s.value());

        }
    }
}
