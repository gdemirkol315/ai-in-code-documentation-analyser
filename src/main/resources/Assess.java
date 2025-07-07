public class Assess() {

    /**
     * Returns the smallest (closest to negative infinity)
     *
     * @param number the number
     * @return the smallest (closest to negative infinity) of given
     * {@code number}
     */
    public static double ceil(double number) {
        if (number - (int) number == 0) {
            return number;
        } else if (number - (int) number > 0) {
            return (int) (number + 1);
        } else {
            return (int) number;
        }
    }

    /**
     * Implements generic bubble sort algorithm.
     *
     * @param array the array to be sorted.
     * @param <T>   the type of elements in the array.
     * @return the sorted array.
     */
    @Override
    public <T extends Comparable<T>> T[] sort(T[] array) {
        for (int i = 1, size = array.length; i < size; ++i) {
            boolean swapped = false;
            for (int j = 0; j < size - i; ++j) {
                if (SortUtils.greater(array[j], array[j + 1])) {
                    SortUtils.swap(array, j, j + 1);
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
        return array;
    }

    /**
     * Get a {@link LogFile} from the given Spring {@link Environment}.
     *
     * @param propertyResolver the {@link PropertyResolver} used to obtain the logging
     *                         properties
     * @return a {@link LogFile} or {@code null} if the environment didn't contain any
     * suitable properties
     */
    public static LogFile get(PropertyResolver propertyResolver) {
        String file = propertyResolver.getProperty(FILE_NAME_PROPERTY);
        String path = propertyResolver.getProperty(FILE_PATH_PROPERTY);
        if (StringUtils.hasLength(file) || StringUtils.hasLength(path)) {
            return new LogFile(file, path);
        }
        return null;
    }

    /**
     * Set value to a widget as a CSSSWTConstants.CSS_CLASS_NAME_KEY value.
     *
     * @param widget
     * @param value
     */
    public static void setCSSClass(Widget widget, String value) {
        widget.setData(CSSSWTConstants.CSS_CLASS_NAME_KEY, value);
    }

    /**
     * Converts value to human readable format
     *
     * @param column column
     * @param value  value
     * @param format string format
     * @return formatted string
     */
    @NotNull
    String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat
            format);


    /**
     * @param mouseButton one of the MOUSE_* constants of this class.
     */
    public void sendMouseEvent(int mouseButton, int column, int row, boolean pressed) {
        if (column < 1) column = 1;
        if (column > mColumns) column = mColumns;
        if (row < 1) row = 1;
        if (row > mRows) row = mRows;

        if (mouseButton == MOUSE_LEFT_BUTTON_MOVED && !isDecsetInternalBitSet(DECSET_BIT_MOUSE_TRACKING_BUTTON_EVENT)) {
            // Do not send tracking.
        } else if (isDecsetInternalBitSet(DECSET_BIT_MOUSE_PROTOCOL_SGR)) {
            mSession.write(String.format("\033[<%d;%d;%d" + (pressed ? 'M' : 'm'), mouseButton, column, row));
        } else {
            mouseButton = pressed ? mouseButton : 3; // 3 for release of all buttons.
            // Clip to screen, and clip to the limits of 8-bit data.
            boolean out_of_bounds = column > 255 - 32 || row > 255 - 32;
            if (!out_of_bounds) {
                byte[] data = {'\033', '[', 'M', (byte) (32 + mouseButton), (byte) (32 + column), (byte) (32 + row)};
                mSession.write(data, 0, data.length);
            }
        }
    }

    /**
     * Constructs and initializes cache with specified capacity and eviction
     * factor. Unacceptable parameter values followed with
     * {@link IllegalArgumentException}.
     *
     * @param maxCapacity    cache max capacity
     * @param evictionFactor cache proceedEviction factor
     */
    @SuppressWarnings("unchecked")
    public LFUCache(final int maxCapacity, final float evictionFactor) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + maxCapacity);
        }
        boolean factorInRange = evictionFactor <= 1 && evictionFactor > 0;
        if (!factorInRange || Float.isNaN(evictionFactor)) {
            throw new IllegalArgumentException("Illegal eviction factor value:" + evictionFactor);
        }
        this.capacity = maxCapacity;
        this.evictionCount = (int) (capacity * evictionFactor);
        this.map = new HashMap<>();
        this.freqTable = new CacheDeque[capacity + 1];
        for (int i = 0; i <= capacity; i++) {
            freqTable[i] = new CacheDeque<>();
        }
        for (int i = 0; i < capacity; i++) {
            freqTable[i].nextDeque = freqTable[i + 1];
        }
        freqTable[capacity].nextDeque = freqTable[capacity];
    }

    /**
     * Return an instance from the context if the type has been registered. The instance
     * will be created if it hasn't been accessed previously.
     *
     * @param <T>  the instance type
     * @param type the instance type
     * @return the instance managed by the context
     * @throws IllegalStateException if the type has not been registered
     */
    <T> T get(Class<T> type) throws IllegalStateException;

    /**
     * Tests whether the {@code /proc/N/environ} file at the given path string contains a specific line prefix.
     *
     * @param envVarFile The path to a /proc/N/environ file.
     * @param key        The env var key to find.
     * @return value The env var value or null.
     */
    private static String readFile(final String envVarFile, final String key) {
        try {
            final byte[] bytes = Files.readAllBytes(Paths.get(envVarFile));
            final String content = new String(bytes, Charset.defaultCharset());
            // Split by null byte character
            final String[] lines = content.split(String.valueOf(CharUtils.NUL));
            final String prefix = key + "=";
            return Arrays.stream(lines)
                    .filter(line -> line.startsWith(prefix))
                    .map(line -> line.split("=", 2))
                    .map(keyValue -> keyValue[1])
                    .findFirst()
                    .orElse(null);
        } catch (final IOException e) {
            return null;
        }
    }

    /**
     * Some visitors can handle symlinks as symlinks. Those visitors should implement
     * this method to provide a different handling for symlink.
     * <p>
     * This method is invoked by those {@link DirScanner}s that can handle symlinks as symlinks.
     * (Not every {@link DirScanner}s are capable of doing that, as proper symlink handling requires
     * letting visitors decide whether or not to descend into a symlink directory.)
     */
    public void visitSymlink(File link, String target, String relativePath) throws IOException {
        visit(link, relativePath);
    }

    /**
     * Handles change in name when committing a direct edit
     */
    @Override
    protected void commitNameChange(PropertyChangeEvent evt) {
        NoteFigure noteFigure = (NoteFigure) getFigure();
        noteFigure.setText(getNote().getObject());
        noteFigure.setVisible(true);
        refreshVisuals();
    }


}