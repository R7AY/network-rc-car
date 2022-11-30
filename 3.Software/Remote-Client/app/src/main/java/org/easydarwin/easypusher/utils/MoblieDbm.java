package org.easydarwin.easypusher.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;

import java.util.List;

public class MoblieDbm {

    @SuppressLint("MissingPermission")
    public static int getMobileDbm(Context context)
    {
        int dbm = -1;
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        List<CellInfo> cellInfoList;




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
        {
            cellInfoList = tm.getAllCellInfo();
            if (null != cellInfoList)
            {
                for (CellInfo cellInfo : cellInfoList)
                {
                    if (cellInfo instanceof CellInfoGsm)
                    {
                        CellSignalStrengthGsm cellSignalStrengthGsm = ((CellInfoGsm)cellInfo).getCellSignalStrength();
                        dbm = cellSignalStrengthGsm.getDbm();
                    }
                    else if (cellInfo instanceof CellInfoCdma)
                    {
                        CellSignalStrengthCdma cellSignalStrengthCdma =
                                ((CellInfoCdma)cellInfo).getCellSignalStrength();
                        dbm = cellSignalStrengthCdma.getDbm();
                    }
                    else if (cellInfo instanceof CellInfoWcdma)
                    {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                        {
                            CellSignalStrengthWcdma cellSignalStrengthWcdma =
                                    ((CellInfoWcdma)cellInfo).getCellSignalStrength();
                            dbm = cellSignalStrengthWcdma.getDbm();
                        }
                    }
                    else if (cellInfo instanceof CellInfoLte)
                    {
                        CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte)cellInfo).getCellSignalStrength();
                        dbm = cellSignalStrengthLte.getDbm();
                    }
                }
            }
        }
        return dbm;
    }
}
