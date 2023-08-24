package dji.v5.ux.core.base.charts.formatter;

import dji.v5.ux.core.base.charts.model.BubbleValue;

public class SimpleBubbleChartValueFormatter implements BubbleChartValueFormatter {
    private ValueFormatterHelper valueFormatterHelper;

    public SimpleBubbleChartValueFormatter() {
        this.valueFormatterHelper = new ValueFormatterHelper();
        this.valueFormatterHelper.determineDecimalSeparator();
    }

    public SimpleBubbleChartValueFormatter(int decimalDigitsNumber) {
        this();
        this.valueFormatterHelper.setDecimalDigitsNumber(decimalDigitsNumber);
    }

    public int formatChartValue(char[] formattedValue, BubbleValue value) {
        return this.valueFormatterHelper.formatFloatValueWithPrependedAndAppendedText(formattedValue, value.getZ(), value.getLabelAsChars());
    }

    public int getDecimalDigitsNumber() {
        return this.valueFormatterHelper.getDecimalDigitsNumber();
    }

    public SimpleBubbleChartValueFormatter setDecimalDigitsNumber(int decimalDigitsNumber) {
        this.valueFormatterHelper.setDecimalDigitsNumber(decimalDigitsNumber);
        return this;
    }

    public char[] getAppendedText() {
        return this.valueFormatterHelper.getAppendedText();
    }

    public SimpleBubbleChartValueFormatter setAppendedText(char[] appendedText) {
        this.valueFormatterHelper.setAppendedText(appendedText);
        return this;
    }

    public char[] getPrependedText() {
        return this.valueFormatterHelper.getPrependedText();
    }

    public SimpleBubbleChartValueFormatter setPrependedText(char[] prependedText) {
        this.valueFormatterHelper.setPrependedText(prependedText);
        return this;
    }

    public char getDecimalSeparator() {
        return this.valueFormatterHelper.getDecimalSeparator();
    }

    public SimpleBubbleChartValueFormatter setDecimalSeparator(char decimalSeparator) {
        this.valueFormatterHelper.setDecimalSeparator(decimalSeparator);
        return this;
    }
}

