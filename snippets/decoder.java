public class MP3Decoder {
    private final int SAMPLE_SIZE_IN_BYTES = 2;
    private final int MICROSECONDS_IN_SECONDS = 1000000;
    private final int BUFFER_WAIT_TIMEOUT = 5000;

    private MediaFormat format;
    private MediaExtractor extractor;
    private MediaCodec decoder;

    /**
     * @param filename audio file to process
     * @param secondsToDecode how many seconds to process
     * @param secondsToSkip how many seconds to skip
     * @return array of PCM data
     * @throws IOException
     */
    public byte[] decode(String filename, int secondsToDecode, int secondsToSkip) throws IOException {
        init(filename);
        byte[] bytes = getDecodedBytes(secondsToDecode, secondsToSkip);
        release();

        return bytes;
    }

    private void init(String filename) throws IOException {
        extractor = new MediaExtractor();
        extractor.setDataSource(filename);
        extractor.selectTrack(0);

        format = extractor.getTrackFormat(0);
        decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
        decoder.configure(format, null, null, 0);
        decoder.start();
    }

    private void release() {
        decoder.stop();
        decoder.release();
        extractor.release();
    }

    public int getSampleRate() {
        return format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
    }

    public int getChannels() {
        return format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
    }

    /**
     * Decodes raw MP3 to byte array
     * @param secondsToDecode how many seconds to process
     * @param secondsToSkip how many seconds to skip
     * @return array of decoded bytes, splited by channels [channel1, channel2, channel1, channel2]
     */
    private byte[] getDecodedBytes(int secondsToDecode, int secondsToSkip) {
        extractor.seekTo(MICROSECONDS_IN_SECONDS * secondsToSkip, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ArrayList<byte[]> dataList = new ArrayList<>();
        int totalLength = 0;

        while (totalLength < secondsToDecode * getSampleRate() * getChannels() * SAMPLE_SIZE_IN_BYTES) {
            int inIndex = decoder.dequeueInputBuffer(BUFFER_WAIT_TIMEOUT);
            if (inIndex >= 0) {
                ByteBuffer buffer = decoder.getInputBuffer(inIndex);
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    decoder.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                    extractor.advance();
                }
            }
            int outIndex = decoder.dequeueOutputBuffer(info, BUFFER_WAIT_TIMEOUT);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    // whatever
                    break;
                default:
                    ByteBuffer buffer = decoder.getOutputBuffer(outIndex);
                    int length = info.size - info.offset;
                    totalLength = totalLength + length;
                    int p = buffer.position();
                    byte[] tempBuffer = new byte[length];
                    buffer.get(tempBuffer, 0, length);
                    dataList.add(tempBuffer);
                    buffer.position(p);
                    decoder.releaseOutputBuffer(outIndex, true);
            }
        }

        byte[] data = new byte[totalLength];
        int offset = 0;
        for (byte[] tempBuffer : dataList) {
            System.arraycopy(tempBuffer, 0, data, offset, tempBuffer.length);
            offset += tempBuffer.length;
        }

        return data;
    }
}