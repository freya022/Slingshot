
#ifndef JNIFSHOTS_STR_UTILS_H
#define JNIFSHOTS_STR_UTILS_H

#include <string>
#include <string_view>

static inline std::string trim(std::string_view v) {
	if (v.length() <= 1) return std::string(v);

	auto start = v.begin();
	auto end = v.begin();

	for (auto it = v.begin(); it != v.end(); it++) {
		if (*it != ' ') {
			start = it;
			break;
		}
	}

	for (auto it = v.begin(); it != v.end(); it++) {
		if (*it != ' ') {
			end = it + 1;
		}
	}

	return std::string(start, end);
}

#endif //JNIFSHOTS_STR_UTILS_H
