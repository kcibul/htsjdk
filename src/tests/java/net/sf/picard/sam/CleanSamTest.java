/*
 * The MIT License
 *
 * Copyright (c) 2014 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.sf.picard.sam;

import net.sf.picard.sam.testers.CleanSamTester;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMValidationError;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

public class CleanSamTest {

    private static final File TEST_DATA_DIR = new File("testdata/net/sf/picard/sam/CleanSam");
    private static final String qualityScore = "&/,&-.1/6/&&)&).)/,&0768)&/.,/874,&.4137572)&/&&,&1-&.0/&&*,&&&&&&&&&&18775799,&16:8775-56256/69::;0";

    @Test(dataProvider = "testCleanSamDataProvider")
    public void testCleanSam(final String samFile, final String expectedCigar) throws IOException {
        final File cleanedFile = File.createTempFile(samFile + ".", ".sam");
        cleanedFile.deleteOnExit();
        final CleanSam cleanSam = new CleanSam();
        cleanSam.INPUT = new File(TEST_DATA_DIR, samFile);
        cleanSam.OUTPUT = cleanedFile;
        Assert.assertEquals(cleanSam.doWork(), 0);
        final SamFileValidator validator = new SamFileValidator(new PrintWriter(System.out), 8000);
        validator.setIgnoreWarnings(true);
        validator.setVerbose(true, 1000);
        validator.setErrorsToIgnore(Arrays.asList(SAMValidationError.Type.MISSING_READ_GROUP));
        SAMFileReader samReader = new SAMFileReader(cleanedFile);
        samReader.setValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
        final SAMRecord rec = samReader.iterator().next();
        samReader.close();
        Assert.assertEquals(rec.getCigarString(), expectedCigar);
        samReader = new SAMFileReader(cleanedFile);
        final boolean validated = validator.validateSamFileVerbose(samReader, null, null);
        samReader.close();
        Assert.assertTrue(validated, "ValidateSamFile failed");
    }

    @DataProvider(name = "testCleanSamDataProvider")
    public Object[][] testCleanSamDataProvider() {
        return new Object[][]{
                {"simple_fits.sam", "100M"},
                {"simple_overhang.sam", "99M1S"},
                {"fits_with_deletion.sam", "91M2D9M"},
                {"overhang_with_deletion.sam", "91M2D8M1S"},
                {"trailing_insertion.sam", "99M1I"},
                {"long_trailing_insertion.sam", "90M10I"},
        };
    }

    //identical test case using the SamFileTester to generate that SAM file on the fly
    @Test(dataProvider = "testCleanSamTesterDataProvider")
    public void testCleanSamTester(final String expectedCigar, final int length, final int alignStart) throws IOException {
        final CleanSamTester cleanSamTester = new CleanSamTester(expectedCigar, length);
        cleanSamTester.addMappedFragment(0, alignStart, false, expectedCigar, qualityScore, -1);
        cleanSamTester.runTest();
    }

    @DataProvider(name = "testCleanSamTesterDataProvider")
    public Object[][] testCleanSamTesterDataProvider() {
        return new Object[][]{
                {"100M", 101, 2},
                {"99M1S", 101, 3},
                {"91M2D9M", 102, 1},
                {"91M2D8M1S", 101, 1},
                {"99M1I", 101, 3},
                {"90M10I", 101, 3},
        };
    }
}