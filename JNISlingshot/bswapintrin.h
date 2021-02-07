
#ifndef JNISLINGSHOT_BSWAPINTRIN_H
#define JNISLINGSHOT_BSWAPINTRIN_H

#ifdef __GNUC__
# include <immintrin.h>
#elif defined(_MSC_VER)
# include <intrin.h>
#include <sstream>

#endif

class Vec128 {
public:
	__m128i v;
	static uint8_t size() { return 16; }
	template<typename STYPE> static Vec128 getMask() {
		switch (sizeof(STYPE)) {
			case 2: return Vec128(_mm_setr_epi8(1, 0, 3, 2, 5, 4, 7, 6, 9, 8, 11, 10, 13, 12, 15, 14));
			case 4: return Vec128(_mm_setr_epi8(3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12));
			case 8: return Vec128(_mm_setr_epi8(7, 6, 5, 4, 3, 2, 1, 0, 15, 14, 13, 12, 11, 10, 9, 8));
			default: return Vec128(_mm_setzero_si128()); // squash warnings.
		}
	}

	Vec128() {};
	Vec128(__m128i const & _v) : v(_v) {};
	static inline void swap(uint8_t* addr, Vec128 mask) {
		__m128i v = _mm_loadu_si128((__m128i*)addr);
		v = _mm_shuffle_epi8(v, mask.v);
		_mm_storeu_si128((__m128i*)addr, v);
	}
};

class Vec256 {
public:
	__m256i v;
	static uint8_t size() { return 32; }
	template<typename STYPE> static Vec256 getMask() {
		return Vec256(_mm256_broadcastsi128_si256(Vec128::getMask<STYPE>().v));
	}

	Vec256() {};
	Vec256(__m256i const & _v) : v(_v) {};
	static inline void swap(uint8_t* addr, Vec256 mask) {
		__m256i v = _mm256_loadu_si256((__m256i*)addr);
		v = _mm256_shuffle_epi8(v, mask.v);
		_mm256_storeu_si256((__m256i*)addr, v);
	}
};

#endif //JNISLINGSHOT_BSWAPINTRIN_H
