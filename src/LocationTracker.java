/*
 * **************************************************************************
 * *                                                                        *
 * * Ericsson hereby grants to the user a royalty-free, irrevocable,        *
 * * worldwide, nonexclusive, paid-up license to copy, display, perform,    *
 * * prepare and have prepared derivative works based upon the source code  *
 * * in this sample application, and distribute the sample source code and  *
 * * derivative works thereof and to grant others the foregoing rights.     *
 * *                                                                        *
 * * ERICSSON DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,        *
 * * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.       *
 * * IN NO EVENT SHALL ERICSSON BE LIABLE FOR ANY SPECIAL, INDIRECT OR      *
 * * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS    *
 * * OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE  *
 * * OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE *
 * * OR PERFORMANCE OF THIS SOFTWARE.                                       *
 * *                                                                        *
 * **************************************************************************
 */

import com.ericsson.hosasdk.utility.framework.FWproxy;
import com.ericsson.hosasdk.api.*;
import com.ericsson.hosasdk.api.mm.*;
import com.ericsson.hosasdk.api.mm.ul.*; 
import com.ericsson.hosasdk.utility.log.*;

public class LocationTracker extends IpAppUserLocationAdapter
    implements IpAppUserLocation
{
    private static final String SERVICE_TYPE_NAME = "P_USER_LOCATION";
    private IpUserLocation itsService;
    private Main theMain;

    public LocationTracker(Main aMain, FWproxy aFWProxy)
    {
        theMain = aMain;
        System.out.println("Getting User Location service interface");
        itsService = (IpUserLocation) aFWProxy.obtainSCF
            (IpUserLocation.class, SERVICE_TYPE_NAME);
    }

    public void dispose()
    {}

    public void getLocation(String anE164Number)
    {
        TpAddress user = new TpAddress(TpAddressPlan.P_ADDRESS_PLAN_E164,
            anE164Number, "",
            TpAddressPresentation.P_ADDRESS_PRESENTATION_ALLOWED,
            TpAddressScreening.P_ADDRESS_SCREENING_USER_VERIFIED_PASSED,
            "");
        TpAddress[] users = new TpAddress[] {user};

        TpLocationRequest request = new TpLocationRequest(100f,
            new TpLocationResponseTime(TpLocationResponseIndicator.P_M_NO_DELAY,
            -1), false, TpLocationType.P_M_CURRENT,
            TpLocationPriority.P_M_NORMAL, "NETWORK");

        itsService.extendedLocationReportReq(this, users, request);			
    }

    public void extendedLocationReportRes(int anAssignmentId, TpUserLocationExtended[] reports)
    {
        for (int i = 0; i != reports.length; i++)
        {
            String user = reports[i].UserID.AddrString;
            if (reports[i].StatusCode == TpMobilityError.P_M_OK)
            {
                float latitude = reports[i].Locations[0].GeographicalPosition.Latitude;
                float longtitude = reports[i].Locations[0].GeographicalPosition.Longitude;
                theMain.notifyLocation(user, latitude, longtitude);
            }
        }
    }

    public void extendedLocationReportErr(int assignmentId, TpMobilityError cause, TpMobilityDiagnostic diagnostic)
    {
        System.out.println("Error reported by extendedLocationReportErr:");
        System.out.println("  Cause = " + ObjectWriter.print(cause));
        System.out.println("  Diagnostic = "
            + ObjectWriter.print(diagnostic));
    }

    public void notImplemented()
    {
        new UnsupportedOperationException("An unexpected callback method was invoked").printStackTrace();
    }
}
