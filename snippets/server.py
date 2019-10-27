import scipy
import math
from scipy.signal import find_peaks
import numpy as np

from flask import Flask
from flask import request

app = Flask(__name__)

@app.route('/', methods=['POST', 'GET'])
def calculate():
    sampleRate = int(request.args.get('sampleRate'))
    channels = int(request.args.get('channels'))

    result = str(bpmaster(request.data, sampleRate, channels))

    return result


def bpmaster(string, sample_rate, channels):
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

    positive_autocorrelation = C[0:5 * sample_rate]

    peaks = find_peaks(positive_autocorrelation, distance=5000)
    result = [x for x in list(map(lambda x: int(60 * sample_rate / x), peaks[0])) if x < 300]

    if (len(result) != 0):
        return result[0]

    return -1


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
