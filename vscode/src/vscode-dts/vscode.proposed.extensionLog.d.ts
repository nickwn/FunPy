/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/

declare module 'vscode' {

	export enum LogLevel {
		Trace = 0,
		Debug = 1,
		Info = 2,
		Warning = 3,
		Error = 4,
		Critical = 5,
		Off = 6
	}

	export namespace env {

		/**
		 * The current log level of the application.
		 */
		export const logLevel: LogLevel;

		/**
		 * An {@link Event} which fires when the log level of the application changes.
		 */
		export const onDidChangeLogLevel: Event<LogLevel>;

	}

	/**
	 * A channel for containing log output.
	 */
	export interface LogOutputChannel extends OutputChannel {

		/**
		 * The current log level of the channel.
		 */
		readonly logLevel: LogLevel;

		/**
		 * An {@link Event} which fires when the log level of the channel changes.
		 */
		readonly onDidChangeLogLevel: Event<LogLevel>;

		/**
		 * Log the given trace message to the channel.
		 *
		 * Messages are only printed when the user has enabled trace logging.
		 *
		 * @param message trace message to log
		 */
		trace(message: string, ...args: any[]): void;
		/**
		 * Log the given debug message to the channel.
		 *
		 * Messages are only printed when the user has enabled debug logging.
		 *
		 * @param message debug message to log
		 */
		debug(message: string, ...args: any[]): void;
		/**
		 * Log the given info message to the channel.
		 *
		 * Messages are only printed when the user has enabled info logging.
		 *
		 * @param message info message to log
		 */
		info(message: string, ...args: any[]): void;
		/**
		 * Log the given warning message to the channel.
		 *
		 * Messages are only printed when the user has enabled warn logging.
		 *
		 * @param message warning message to log
		 */
		warn(message: string, ...args: any[]): void;
		/**
		 * Log the given error or error message to the channel.
		 *
		 * Messages are only printed when the user has enabled error logging.
		 *
		 * @param error Error or error message to log
		 */
		error(error: string | Error, ...args: any[]): void;
	}

	export namespace window {
		/**
		 * Creates a new {@link LogOutputChannel log output channel} with the given name.
		 *
		 * @param name Human-readable string which will be used to represent the channel in the UI.
		 * @param options Options for the log output channel.
		 */
		export function createOutputChannel(name: string, options: { readonly log: true }): LogOutputChannel;
	}

}
