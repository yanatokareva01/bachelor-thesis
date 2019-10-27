def bpmaster(string, sample_rate):
    samples = np \
        .frombuffer(string, dtype=np.dtype([('re', np.int16), ('im', np.int16)])) \
        .view(np.int16).astype(np.float32).view(np.complex64)

    rms_samples = list(map(lambda x: abs(x), samples))

    next_power_of_two = math.ceil(math.log(len(rms_samples), 2))

    deficit = int(math.pow(2, next_power_of_two) - len(rms_samples))
    arr = np.concatenate((np.zeros(deficit), rms_samples))

    A = np.fft.fft(arr)
    S = A * np.conj(A)
    C = np.fft.ifft(S)

    positive_autocorrelation = C[0:2 * sample_rate]

    peaks = find_peaks(positive_autocorrelation, distance=5000, width=25000, rel_height=10)
    result = [x for x in list(map(lambda x: int(60 * sample_rate / x), peaks[0])) if x < 300]

    return result