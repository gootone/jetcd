/**
 * Copyright 2017 The jetcd authors
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

package com.coreos.jetcd.op;

import static com.coreos.jetcd.options.OptionsUtil.toRangeRequestSortOrder;
import static com.coreos.jetcd.options.OptionsUtil.toRangeRequestSortTarget;

import com.coreos.jetcd.api.DeleteRangeRequest;
import com.coreos.jetcd.api.PutRequest;
import com.coreos.jetcd.api.RangeRequest;
import com.coreos.jetcd.api.RequestOp;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.options.DeleteOption;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import com.google.protobuf.ByteString;

/**
 * Etcd Operation.
 */
public abstract class Op {

  /**
   * Operation type.
   */
  public enum Type {
    PUT, RANGE, DELETE_RANGE,
  }

  protected final Type type;
  protected final ByteString key;

  protected Op(Type type, ByteString key) {
    this.type = type;
    this.key = key;
  }

  abstract RequestOp toRequestOp();

  public static PutOp put(ByteSequence key, ByteSequence value, PutOption option) {
    return new PutOp(ByteString.copyFrom(key.getBytes()), ByteString.copyFrom(value.getBytes()),
        option);
  }

  public static GetOp get(ByteSequence key, GetOption option) {
    return new GetOp(ByteString.copyFrom(key.getBytes()), option);
  }

  public static DeleteOp delete(ByteSequence key, DeleteOption option) {
    return new DeleteOp(ByteString.copyFrom(key.getBytes()), option);
  }

  public static final class PutOp extends Op {

    private final ByteString value;
    private final PutOption option;

    protected PutOp(ByteString key, ByteString value, PutOption option) {
      super(Type.PUT, key);
      this.value = value;
      this.option = option;
    }

    RequestOp toRequestOp() {
      PutRequest put = PutRequest.newBuilder()
          .setKey(this.key)
          .setValue(this.value)
          .setLease(this.option.getLeaseId())
          .setPrevKv(this.option.getPrevKV())
          .build();

      return RequestOp.newBuilder().setRequestPut(put).build();
    }
  }

  public static final class GetOp extends Op {

    private final GetOption option;

    protected GetOp(ByteString key, GetOption option) {
      super(Type.RANGE, key);
      this.option = option;
    }

    RequestOp toRequestOp() {
      RangeRequest.Builder range = RangeRequest.newBuilder()
          .setKey(this.key)
          .setCountOnly(this.option.isCountOnly())
          .setLimit(this.option.getLimit())
          .setRevision(this.option.getRevision())
          .setKeysOnly(this.option.isKeysOnly())
          .setSerializable(this.option.isSerializable())
          .setSortOrder(toRangeRequestSortOrder(this.option.getSortOrder()))
          .setSortTarget(toRangeRequestSortTarget(this.option.getSortField()));

      this.option.getEndKey()
          .ifPresent(endkey -> range.setRangeEnd(ByteString.copyFrom(endkey.getBytes())));

      return RequestOp.newBuilder().setRequestRange(range).build();
    }
  }

  public static final class DeleteOp extends Op {

    private final DeleteOption option;

    protected DeleteOp(ByteString key, DeleteOption option) {
      super(Type.DELETE_RANGE, key);
      this.option = option;
    }

    RequestOp toRequestOp() {
      DeleteRangeRequest.Builder delete = DeleteRangeRequest.newBuilder()
          .setKey(this.key)
          .setPrevKv(this.option.isPrevKV());

      if (this.option.getEndKey().isPresent()) {
        delete.setRangeEnd(ByteString.copyFrom(this.option.getEndKey().get().getBytes()));
      }

      return RequestOp.newBuilder().setRequestDeleteRange(delete).build();
    }
  }
}