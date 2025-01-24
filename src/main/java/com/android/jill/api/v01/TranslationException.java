/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.jill.api.v01;

import javax.annotation.Nonnull;
import java.io.Serial;

/**
 * A fatal problem that caused Jill to abort the translation. The problem should already have
 * reported, so it is safe to ignore its message.
 */
public class TranslationException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link TranslationException} with no detail message.
     */
    public TranslationException() {
        super();
    }

    /**
     * @param message the detail message. The detail message is saved for later retrieval by the
     */
    public TranslationException(@Nonnull String message) {
        super(message);
    }

    /**
     * @param message the detail message. The detail message is saved for later retrieval by the
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public TranslationException(@Nonnull String message, @Nonnull Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public TranslationException(@Nonnull Throwable cause) {
        super(cause);
    }
}
