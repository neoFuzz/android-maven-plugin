/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.jack.api.v01;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Abstract class to easily chain exceptions together.
 * <p>
 * The exception can be managed like any other exception. In this case, the first one will be the
 * only one used.
 * <p>
 * Special management can use the {@link #iterator()} or the {@link #getNextException()} to browse
 * all chained exceptions and dispatch them.
 * <p>
 * See {@link ChainedExceptionBuilder} to build the chain of exceptions.
 */
public abstract class ChainedException extends Exception
        implements Iterable<ChainedException> {
    /**
     * Serial version UID for serialization.
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The message of the exception.
     */
    @Nonnull
    private String message;

    /**
     * The number of chained exceptions.
     */
    @Nonnegative
    private int count = 1;

    /**
     * The last chained exception.
     */
    @Nonnull
    private ChainedException tail = this;

    /**
     * The next chained exception.
     */
    @CheckForNull
    private ChainedException next = null;

    /**
     * @param message the message of the exception
     */
    public ChainedException(@Nonnull String message) {
        super("");
        this.message = message;
    }

    /**
     * @param message the message of the exception
     * @param cause   the cause of the exception
     */
    public ChainedException(@Nonnull String message, @Nonnull Throwable cause) {
        super("", cause);
        this.message = message;
    }

    /**
     * @param cause the cause of the exception
     */
    public ChainedException(@Nonnull Throwable cause) {
        super(cause);
        this.message = cause.getMessage();
    }

    /**
     * @return the message of the exception.
     */
    @Override
    @Nonnull
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(@Nonnull String message) {
        this.message = message;
    }

    /**
     * @return the localized message of the exception.
     */
    @Override
    @Nonnull
    public String getLocalizedMessage() {
        return getMessage();
    }

    /**
     * @param head the head of the chain of exceptions
     * @return the head of the chain of exceptions
     */
    @Nonnull
    protected ChainedException putAsLastExceptionOf(
            @CheckForNull ChainedException head) {
        if (head == null) {
            this.tail = this;
            this.next = null;
            this.count = 1;

            return this;
        } else {
            head.tail.next = this;
            head.tail = this;
            head.count++;

            return head;
        }
    }

    /**
     * @return the next chained exception or null if there is no next exception
     */
    @CheckForNull
    public ChainedException getNextException() {
        return next;
    }

    /**
     * @return the number of chained exceptions
     */
    @Nonnegative
    public int getNextExceptionCount() {
        return count;
    }

    /**
     * @return an iterator to browse all chained exceptions
     */
    @Override
    @Nonnull
    public Iterator<ChainedException> iterator() {
        ArrayList<ChainedException> list = new ArrayList<>(count);

        ChainedException exception = this;
        do {
            list.add(exception);
            exception = exception.next;
        } while (exception != null);

        return list.iterator();
    }

    /**
     * Builder to construct a chain of exceptions.
     */
    public static class ChainedExceptionBuilder<T extends ChainedException> {
        /**
         * head
         */
        @CheckForNull
        private T head = null;

        /**
         * @param exceptions the exceptions to add to the chain
         */
        @SuppressWarnings("unchecked")
        public void appendException(@Nonnull T exceptions) {
            for (ChainedException exception : exceptions) {
                head = (T) exception.putAsLastExceptionOf(head);
            }
        }

        /**
         * @throws T the first exception in the chain
         */
        public void throwIfNecessary() throws T {
            if (head != null) {
                throw head;
            }
        }

        /**
         * @return the first exception in the chain
         */
        @Nonnull
        public T getException() {
            assert head != null;
            return head;
        }
    }
}
