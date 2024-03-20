# Algo Trading Analytics Tool - Breakout Failure Pattern Identification

## Overview

This Java project serves as an algo trading analytics tool designed to identify breakout failure patterns in financial markets and notify users accordingly. Breakout failure patterns occur when a financial instrument's price breaks out of a defined range but fails to maintain momentum, often indicating potential reversal or continuation of the previous trend. This tool aims to assist traders and analysts in making informed decisions by detecting such patterns and providing timely notifications.
## Breakout Failure Chart: Double Top Failure and Breakout in 3rd Attempt

![Breakout Failure Chart](https://www.tradingview.com/i/eAr5qpYN/)

This chart illustrates a double top failure pattern followed by a breakout in the third attempt. In a double top pattern, the price reaches a high point, retraces, and then fails to break above the previous high, indicating potential weakness. In this example, the price attempts to break out twice but fails before successfully breaking out on the third attempt.

## Features

- **Breakout Failure Detection:** Utilizes algorithms to detect breakout failure patterns in historical price data.

- **Real-time Monitoring:** Monitors live market data for potential breakout failure patterns.

- **Notification System:** Notifies users through various channels (e.g., email, SMS) upon detecting a breakout failure pattern.

- **Configurability:** Allows users to customize detection parameters and notification preferences.

## Angel Smart API - Dependency
- For accessing market data, this project utilizes the Angel Smart API provided by Angel Broking. You can find more information about the Angel Smart API [here](https://smartapi.angelbroking.com/docs).
- Initially, I explored using Yahoo Finance and Google APIs to obtain market data for India-listed equities. However, I quickly discovered that these APIs weren't reliable for my needs. After researching alternative options, I found that Angel Broking offers a free API for accessing market data. Unlike other providers such as Zerodha, Angel Broking's API does not have associated charges, making it an attractive choice for this project.
- Note that I am not using Java SDK offered by them and instead just going with plain Rest/HTTP. I found that the SDK isnt bug free yet and that it isnt published accurately in maven repo

## Contributing
Contributions are welcome! If you encounter any issues or have suggestions for improvements, please submit a pull request or open an issue on the GitHub repository.

## License
This project is licensed under the MIT License.

## Disclaimer
This tool is provided for informational purposes only and should not be considered financial advice. Users are solely responsible for their trading decisions and should conduct thorough research and analysis before making any investment.

## Contact
For any inquiries or support, please contact viren.chande@gmail.com

## Pre-requisites

- Java Development Kit (JDK) version 21 or higher and preview enables for virtual threads and Structured tasks.
- Git installed on your local machine.
- Account with angel broking and TOTP enabled account https://smartapi.angelbroking.com/enable-totp

## Sample

- Use swagger for now to detect failure double top for now before we make a UI
![Alt text](/swagger-ss.png?raw=true "Shows list of nse tokens / symbols that match the pattern")
- Here's one sample from our codebase![CINEVISTA](https://www.tradingview.com/i/poLLBtUI/)
- Sample output message in Telegram group by the integrated bot
  ![Alt text](/Telegram.jpg?raw=true "Telegram message")
## TODO
- Notification may be weekly on whatsapp/telegram. Dont read emails much these days
- Google has a TOTP library to automate the totp generation but dont feel like using it to automate totp. There is a reason its designed that way.


