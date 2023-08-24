package dji.v5.ux.core.base.charts.formatter;

import android.util.Log;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import dji.v5.ux.core.base.charts.util.FloatUtils;

public class ValueFormatterHelper {
    private int decimalDigitsNumber = Integer.MIN_VALUE;
    private char[] appendedText = new char[0];
    private char[] prependedText = new char[0];
    private char decimalSeparator = '.';

    public ValueFormatterHelper() {
        //do nothing
    }

    public void determineDecimalSeparator() {
        NumberFormat numberFormat = NumberFormat.getInstance();
        if (numberFormat instanceof DecimalFormat) {
            this.decimalSeparator = ((DecimalFormat)numberFormat).getDecimalFormatSymbols().getDecimalSeparator();
        }

    }

    public int getDecimalDigitsNumber() {
        return this.decimalDigitsNumber;
    }

    public ValueFormatterHelper setDecimalDigitsNumber(int decimalDigitsNumber) {
        this.decimalDigitsNumber = decimalDigitsNumber;
        return this;
    }

    public char[] getAppendedText() {
        return this.appendedText;
    }

    public ValueFormatterHelper setAppendedText(char[] appendedText) {
        if (null != appendedText) {
            this.appendedText = appendedText;
        }

        return this;
    }

    public char[] getPrependedText() {
        return this.prependedText;
    }

    public ValueFormatterHelper setPrependedText(char[] prependedText) {
        if (null != prependedText) {
            this.prependedText = prependedText;
        }

        return this;
    }

    public char getDecimalSeparator() {
        return this.decimalSeparator;
    }

    public ValueFormatterHelper setDecimalSeparator(char decimalSeparator) {
        char nullChar = 0;
        if (nullChar != decimalSeparator) {
            this.decimalSeparator = decimalSeparator;
        }

        return this;
    }

    public int formatFloatValueWithPrependedAndAppendedText(char[] formattedValue, float value, int defaultDigitsNumber, char[] label) {
        int labelLength;
        if (null != label) {
            labelLength = label.length;
            if (labelLength > formattedValue.length) {
                Log.w("ValueFormatterHelper", "Label length is larger than buffer size(64chars), some chars will be skipped!");
                labelLength = formattedValue.length;
            }

            System.arraycopy(label, 0, formattedValue, formattedValue.length - labelLength, labelLength);
            return labelLength;
        } else {
            labelLength = this.getAppliedDecimalDigitsNumber(defaultDigitsNumber);
            int charsNumber = this.formatFloatValue(formattedValue, value, labelLength);
            this.appendText(formattedValue);
            this.prependText(formattedValue, charsNumber);
            return charsNumber + this.getPrependedText().length + this.getAppendedText().length;
        }
    }

    public int formatFloatValueWithPrependedAndAppendedText(char[] formattedValue, float value, char[] label) {
        return this.formatFloatValueWithPrependedAndAppendedText(formattedValue, value, 0, label);
    }

    public int formatFloatValueWithPrependedAndAppendedText(char[] formattedValue, float value, int defaultDigitsNumber) {
        return this.formatFloatValueWithPrependedAndAppendedText(formattedValue, value, defaultDigitsNumber, (char[])null);
    }

    public int formatFloatValue(char[] formattedValue, float value, int decimalDigitsNumber) {
        return FloatUtils.formatFloat(formattedValue, value, formattedValue.length - this.appendedText.length, decimalDigitsNumber, this.decimalSeparator);
    }

    public void appendText(char[] formattedValue) {
        if (this.appendedText.length > 0) {
            System.arraycopy(this.appendedText, 0, formattedValue, formattedValue.length - this.appendedText.length, this.appendedText.length);
        }

    }

    public void prependText(char[] formattedValue, int charsNumber) {
        if (this.prependedText.length > 0) {
            System.arraycopy(this.prependedText, 0, formattedValue, formattedValue.length - charsNumber - this.appendedText.length - this.prependedText.length, this.prependedText.length);
        }

    }

    public int getAppliedDecimalDigitsNumber(int defaultDigitsNumber) {
        int appliedDecimalDigitsNumber;
        if (this.decimalDigitsNumber < 0) {
            appliedDecimalDigitsNumber = defaultDigitsNumber;
        } else {
            appliedDecimalDigitsNumber = this.decimalDigitsNumber;
        }

        return appliedDecimalDigitsNumber;
    }
}

