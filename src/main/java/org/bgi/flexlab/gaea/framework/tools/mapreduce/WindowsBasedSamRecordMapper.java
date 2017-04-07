package org.bgi.flexlab.gaea.framework.tools.mapreduce;

import htsjdk.samtools.SAMRecord;
import org.apache.hadoop.mapreduce.Mapper;
import org.bgi.flexlab.gaea.data.mapreduce.writable.SamRecordWritable;

/**
 * Created by zhangyong on 2017/3/8.
 */
public class WindowsBasedSamRecordMapper extends WindowsBasedMapper<SamRecordWritable>{

    @Override
    void otherSetup(Context context) {

    }

    @Override
    void setOutputValue(SAMRecord samRecord) {
        outputValue.set(samRecord);
    }

    @Override
    void initOutputVaule() {
        outputValue = new SamRecordWritable();
    }
}
