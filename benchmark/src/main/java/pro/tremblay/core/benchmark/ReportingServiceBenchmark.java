/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pro.tremblay.core.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import pro.tremblay.core.Amount;
import pro.tremblay.core.PriceService;
import pro.tremblay.core.Quantity;
import pro.tremblay.core.ReportingService;
import pro.tremblay.core.Security;
import pro.tremblay.core.SecurityPosition;
import pro.tremblay.core.SecurityService;
import pro.tremblay.core.Transaction;
import pro.tremblay.core.TransactionType;
import pro.tremblay.core.Percentage;
import pro.tremblay.core.Position;
import pro.tremblay.core.Preferences;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static pro.tremblay.core.Position.position;
import static pro.tremblay.core.Quantity.qty;
import static pro.tremblay.core.SecurityPosition.securityPosition;
import static pro.tremblay.core.Transaction.transaction;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
@State(Scope.Benchmark)
public class ReportingServiceBenchmark {

    private final Preferences preferences = new Preferences();
    private final Clock clock = Clock.systemUTC();
    private final SecurityService securityService = new SecurityService(Path.of("../listing_status.csv"));
    private final PriceService priceService = new PriceService(securityService, clock);
    private final ReportingService service = new ReportingService(preferences, clock, priceService);

    private Collection<Transaction> transactions;
    private Position position;

    @Setup
    public void setup() {
        preferences.put(ReportingService.LENGTH_OF_YEAR, "365");

        List<Security> securities = securityService.allSecurities();
        SecurityPosition[] securityPositions = securities.stream()
            .map(sec -> securityPosition(sec, qty(1_000)))
            .toArray(SecurityPosition[]::new);

        position = position()
            .cash(Amount.amnt(1_000_000))
            .addSecurityPositions(securityPositions);

        LocalDate now = LocalDate.now();
        int dayOfYear = now.getDayOfYear();

        TransactionType[] transactionTypes = TransactionType.values();

        Random random = new Random();
        transactions = random.ints(100, 1, 100)
            .mapToObj(quantity -> {
                Transaction t = transaction();
                return t
                    .date(now.minusDays(random.nextInt(dayOfYear)))
                    .cash(Amount.amnt(random.nextInt(1_000)))
                    .type(transactionTypes[random.nextInt(transactionTypes.length)])
                    .quantity(t.getType().hasQuantity() ? qty(quantity) : Quantity.zero())
                    .security(t.getType().hasQuantity() ? securities.get(random.nextInt(securities.size())) : null);
            })
            .collect(Collectors.toList());
    }

    @Benchmark
    public Percentage calculate() {
        return service.calculateReturnOnInvestmentYTD(position, transactions);
    }

    public static void main(String[] args) {
        ReportingServiceBenchmark benchmark = new ReportingServiceBenchmark();
        benchmark.setup();
        benchmark.calculate();
    }
}
