//
// Created by dev on 7/25/20.
//

#ifndef MEINKRAFT_UTILS_H
#define MEINKRAFT_UTILS_H

#include <string>
#include <chrono>
#include <iostream>

#ifndef _WIN32
	#define min std::min
	#define max std::max
#endif

template<typename T>
static void measureTime(const std::string &desc, int iterations, T code) {
	double worst = (1 << 16) / -1.0, best = (1 << 16) / 1.0, total = 0, average;

	for (int i = 0; i < iterations; i++) {
		auto start = std::chrono::system_clock::now();

		code();

		auto end = std::chrono::system_clock::now();

		double elapsed = std::chrono::duration_cast<std::chrono::nanoseconds>(end - start).count() / 1000000.0;
		worst = max(worst, elapsed);
		best = min(best, elapsed);
		total += elapsed;
	}

	average = total / iterations;

	char buf[1024];
	std::string buffer = desc + " : Iterations : %s, Best : %s ms, Worst : %s ms, Average : %s ms, Total : %s ms";
	std::sprintf(buf, buffer.data(), std::to_string(iterations).c_str(), std::to_string(best).c_str(), std::to_string(worst).c_str(), std::to_string(average).c_str(), std::to_string(total).c_str());

	std::cout << buf << std::endl;
}

#endif //MEINKRAFT_UTILS_H
