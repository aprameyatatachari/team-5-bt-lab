package com.btlab.fdcalculator.controller;

import com.btlab.fdcalculator.model.dto.FDCalculationRequest;
import com.btlab.fdcalculator.model.dto.FDCalculationResponse;
import com.btlab.fdcalculator.repository.FDCalculationInputRepository;
import com.btlab.fdcalculator.service.FDCalculatorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Fixed Deposit calculations
 * Provides endpoints for calculating FD returns and retrieving calculation history
 */
@RestController
@RequestMapping("/api/fd")
@RequiredArgsConstructor
@Tag(name = "FD Calculator", description = "Fixed Deposit calculation endpoints for both cumulative and non-cumulative FDs")
public class FDCalculatorController {

    private final FDCalculatorService fdCalculatorService;
    private final FDCalculationInputRepository inputRepo;

    @Operation(
        summary = "Calculate Fixed Deposit returns",
        description = """
            Calculate Fixed Deposit maturity value, interest, and APY based on provided parameters.
            
            **Supports two FD types:**
            
            1. **Cumulative FD** (cumulative=true):
               - Interest is compounded and reinvested
               - Returns maturity_value with accumulated interest
               - Customer receives full amount at maturity
            
            2. **Non-Cumulative FD** (cumulative=false):
               - Interest is paid out periodically
               - Returns payout_freq and payout_amount
               - Principal remains constant
            
            **Product Configuration:**
            - Interest type and compounding frequency are automatically fetched from Product & Pricing API
            - These values are retrieved from /api/products/{productCode} endpoint
            - Manual override is possible by providing these fields in the request
            
            **Interest Rate Calculation:**
            - Base rate fetched from Product & Pricing API based on tenure
            - Additional rates applied based on customer categories
            - Effective rate = Base rate + Category benefits
            - APY calculated based on compounding frequency
            
            **Example Scenarios:**
            
            **Senior Citizen - Cumulative:**
            - Principal: ₹1,00,000
            - Tenure: 5 years
            - Category: SENIOR (+0.75%)
            - Compounding: Quarterly
            - Result: Higher maturity value due to compounding
            
            **Gold Customer - Non-Cumulative:**
            - Principal: ₹50,000
            - Tenure: 3 years
            - Category: GOLD (+0.25%)
            - Payout: Quarterly
            - Result: Quarterly interest payments
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "FD calculation parameters",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FDCalculationRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Cumulative FD - Senior Citizen",
                        summary = "Senior citizen cumulative FD (interest_type and compounding_frequency fetched from product)",
                        value = """
                            {
                              "principal_amount": 100000,
                              "tenure_value": 5,
                              "tenure_unit": "YEARS",
                              "currency_code": "INR",
                              "category1_id": "SENIOR",
                              "category2_id": "GOLD",
                              "cumulative": true,
                              "product_code": "FD001"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Non-Cumulative FD - Gold Customer",
                        summary = "Gold customer non-cumulative FD with yearly payouts (interest_type and compounding_frequency fetched from product)",
                        value = """
                            {
                              "principal_amount": 50000,
                              "tenure_value": 3,
                              "tenure_unit": "YEARS",
                              "currency_code": "INR",
                              "category1_id": "GOLD",
                              "cumulative": false,
                              "payout_freq": "YEARLY",
                              "product_code": "FD001"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Employee FD - Monthly Payout",
                        summary = "Employee non-cumulative FD with monthly interest payout (interest_type and compounding_frequency fetched from product)",
                        value = """
                            {
                              "principal_amount": 200000,
                              "tenure_value": 2,
                              "tenure_unit": "YEARS",
                              "currency_code": "INR",
                              "category1_id": "EMP",
                              "cumulative": false,
                              "payout_freq": "MONTHLY",
                              "product_code": "FD001"
                            }
                            """
                    )
                }
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Calculation successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FDCalculationResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Cumulative FD Result",
                        summary = "Result for cumulative FD",
                        value = """
                            {
                              "maturity_value": 164361.50,
                              "maturity_date": "2030-10-10",
                              "apy": 10.6508,
                              "effective_rate": 10.2500,
                              "payout_freq": null,
                              "payout_amount": null,
                              "calc_id": 123,
                              "result_id": 123
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Non-Cumulative FD Result",
                        summary = "Result for non-cumulative FD with payouts",
                        value = """
                            {
                              "maturity_value": 50000.00,
                              "maturity_date": "2028-10-10",
                              "apy": 10.6508,
                              "effective_rate": 10.2500,
                              "payout_freq": "YEARLY",
                              "payout_amount": 5325.38,
                              "calc_id": 124,
                              "result_id": 124
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters (validation failed)",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error (e.g., Product & Pricing API unavailable)",
            content = @Content(mediaType = "application/json")
        )
    })
    @PostMapping("/calculate")
    public FDCalculationResponse calculate(@RequestBody FDCalculationRequest request) {
        return fdCalculatorService.calculate(request);
    }

    @Operation(
        summary = "Get calculation by ID",
        description = """
            Retrieve a previously calculated FD result using its calculation ID.
            
            **Use Case:**
            - Review past calculations
            - Compare different FD scenarios
            - Retrieve customer's FD calculation history
            
            **Response includes:**
            - All calculation inputs (principal, tenure, categories)
            - Calculated results (maturity value, APY, effective rate)
            - Payout details for non-cumulative FDs
            """,
        parameters = {
            @Parameter(
                name = "calcId",
                description = "Unique calculation ID returned from the calculate endpoint",
                required = true,
                example = "123"
            )
        }
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Calculation found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = FDCalculationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Calculation not found",
            content = @Content(mediaType = "application/json")
        )
    })
    @GetMapping("/calculations/{calcId}")
    public FDCalculationResponse getOne(
        @PathVariable 
        @Parameter(description = "Calculation ID", example = "123") 
        Long calcId
    ) {
        return fdCalculatorService.getByCalcId(calcId);
    }

    @Operation(
        summary = "Get calculation history",
        description = """
            Get a list of all calculation IDs in the system.
            
            **Use Case:**
            - View all past calculations
            - Audit trail for FD calculations
            - Customer calculation history
            
            **Returns:**
            List of calculation IDs that can be used with the /calculations/{calcId} endpoint to retrieve full details.
            
            **Note:** This endpoint returns only the IDs. Use the individual get endpoint to retrieve full calculation details.
            """,
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "List of calculation IDs",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Calculation IDs",
                        value = "[101, 102, 103, 104, 105]"
                    )
                )
            )
        }
    )
    @GetMapping("/history")
    public List<Long> history() {
        return inputRepo.findAll().stream().map(i -> i.getCalcId()).toList();
    }
}
